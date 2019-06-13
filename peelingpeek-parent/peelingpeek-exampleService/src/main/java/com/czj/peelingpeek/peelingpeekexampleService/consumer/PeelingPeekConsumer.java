package com.czj.peelingpeek.peelingpeekexampleService.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.czj.peelingpeek.peelingpeekexampleService.consumerConfigure.DefaultPullConsumerConfigure;
import com.czj.peelingpeek.peelingpeekexampleService.entity.TestBean;
import com.czj.peelingpeek.peelingpeekexampleService.service.TestService;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

/**
 * @Author: clownc
 * @Date: 2019-06-12 15:05
 */
@Log4j2
@Configuration
public class PeelingPeekConsumer extends DefaultPullConsumerConfigure implements ApplicationRunner {

    @Autowired
    private TestService testService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value("${rocketmq.topic.getAccountTopic}")
    private String getAccountTopic;
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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            super.listener(getAccountTopic, "*");
        } catch (MQClientException e) {
            log.error("消费者监听器启动失败", e);
        }
    }
}
