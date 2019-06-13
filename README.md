##### #### # peelingpeek 流量削峰
#### 前言
在学习怎么使用rocketmq的过程中，了解了mq的使用场景，系统解耦，异步发送消息，流量削峰等，在网上找了很久使用rocketmq做流量削峰的案例，没有找到，只看到了几个解决思路，然后选了其中一个来做了此案例。
#### 环境
- springboot 2.0.x
- springcloud Finchley.RELEASE
- nacos （阿里巴巴基于springcloud做的服务发现注册中心，类似springcloud的eureka），[安装使用可参照nacos官网](https://nacos.io/zh-cn/docs/quick-start.html)
- redis
- rocketmq4.5
#### 思路
http请求（这里以post请求为例）->

gateway 网关 通过RouteLocator配置路由信息将对应要削峰的路由的请求转发到mq服务->

mq服务接收到数据后，将数据发送到对应topic。->

被削峰的请求所在服务开启mq消费者（MQPullConsumerScheduleService，负载均衡pull请求）监听消息，每隔一定时间拉取到消息后在服务内进行业务处理->

处理完毕后将msgId作为key，处理结果作为value存入redis。->

mq服务发送消息的线程在发送完消息并且返回为发送成功后线程挂起，每隔一秒根据msgId去redis查找消费结果，设定挂起时间为30s（可以随便设定，也可以再写一个定时任务服务去查结果），查到结果则返回，没查到则请求返回超时。

**关于权限这一块：**
- 因为gateway现在大部分都是结合jwt做权限认证
- 如果需要削峰的请求是需要权限认证，则在网关的全局过滤器拿到jwt，再在全局过滤器中调用auth服务
- 根据token在auth服务中拿到权限信息，然后在判断用户是否拥有请求该路由的权限，没有则返回无权访问，有就执行filer.chain
- request.getheaders().get("authentication")->authService.getPermission(token)->checkUserPermission(permission,requestUri,user)
- 这里因为权限服务比较复杂，直接对请求的路由放行（等同于login请求一样直接放行）

![image](https://github.com/czj940548563/images/blob/master/1560395419104.png?raw=true)


#### 服务：
- peelingpeek-gateway 网关服务
- peelingpeek-mq  消息服务
- peelingpeek-exampleService 原请求目标服务

#### peelingpeek-gateway
##### gateway全局过滤器，用户是否拥有权限一般在这处理
```
@Configuration
public class AccessGatewayFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestUri = request.getPath().pathWithinApplication().value();
        /**
         * 如果需要削峰的请求是需要权限认证，则在网关的全局过滤器拿到jwt，再在全局过滤器中调用auth服务
         * 根据token拿到权限信息返回，然后在判断用户是否拥有请求该路由的权限，没有则返回无权访问，有就执行filer.chain
         * request.getheaders().get("authentication")->authService.getPermission(token)->checkUserPermission(permission,requestUri,user)
         * 这里因为权限服务比较复杂，直接对请求的路由放行
         */
        if (requestUri.contains("/getAccount")){
            return chain.filter(exchange);//直接放行
        }else {//需要权限认证则走这里,进行权限认证
            return getVoidMono(exchange, new TokenForbiddenResponse("User Forbidden!Does not has Permission!"));
        }
    }
    /**
     * 网关抛异常
     *
     * @param body
     */
    private Mono<Void> getVoidMono(ServerWebExchange serverWebExchange, BaseResponse body) {
        serverWebExchange.getResponse().setStatusCode(HttpStatus.OK);
        byte[] bytes = JSONObject.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = serverWebExchange.getResponse().bufferFactory().wrap(bytes);
        return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
    }

```
##### RouteLocatorConfig路由配置
将要削峰处理的请求转发到mq服务
```
@Configuration
public class RouteLocatorConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                //这里转发的路由可以弄成动态路由的形式来管理，在数据库中存储。
                .route(r->r.path("/api/exampleService/getAccount").uri("http://127.0.0.1:6999/api/mq/getAccount"))
                .build();

    }

}
```
#### peelingpeek-mq
##### MqController
发送请求中携带的数据到消息队列，并查找消费结果

```
      try {
            String jsonString = JSON.toJSONString(jsonData);
            Message msg = new Message(getAccountTopic /* Topic */,
                    "*" /* Tag */,
                    jsonString.getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
            );
            SendResult sendResult = defaultMQProducer.send(msg);
            String msgId = sendResult.getMsgId();
            SendStatus sendStatus = sendResult.getSendStatus();
            /**
             * 如果sendStatus是发送成功的状态，则在30秒内尝试在redis根据msgId获取消费者业务处理的结果,15秒后没拿到则返回超时
             */
            int i=30;
            String consumeResult=null;
            if (StringUtils.equals(sendStatus.toString(),"SEND_OK")){
                while (i>0){
                    consumeResult = stringRedisTemplate.opsForValue().get(msgId);
                    if (consumeResult!=null) {
                        BaseResponse baseResponse = new BaseResponse();
                        baseResponse.setMessage(consumeResult+msgId);
                        return baseResponse;
                    }
                    Thread.sleep(1000);
                    i--;
                }
                return new BaseResponse(500001,"请求返回超时");
            }


            System.out.printf("%s%n", sendStatus);
        } catch (Exception e) {
            e.printStackTrace();
            Thread.sleep(1000);
        }
        return new BaseResponse(500,"生产者发送消息失败");
```
#### peelingpeek-exampleService
#####  PeelingPeekConsumer
从topic中拉取消息，并进行业务处理，得到结果够放入缓存

```
  @Override
    public void dealBody(List<MessageExt> msgs) {
        //从消息队列拿到数据进行业务处理
        log.info("拉取消息");
        msgs.stream().forEach(msg -> {
            try {
                String msgStr = new String(msg.getBody(), "utf-8");
                JSONObject jsonObject = JSON.parseObject(msgStr);
                // Map<String, Object> parameterMap = (Map<String, Object>) jsonObject.get("parameterMap");如果url中带有参数
                TestBean testBean = new TestBean();
                testBean.setAccount(jsonObject.getString("account"));
                /**
                 * 这里可以进行业务操作
                 */
                String operationResult = testService.operation(testBean);
                /**
                 * 将业务操作的结果放入redis缓存
                 */
                stringRedisTemplate.opsForValue().set(msg.getMsgId(),operationResult);
                log.info(operationResult+msg.getMsgId()+new Date());
            } catch (UnsupportedEncodingException e) {
                log.error("body转字符串解析失败");
            }
        });
    }
```





