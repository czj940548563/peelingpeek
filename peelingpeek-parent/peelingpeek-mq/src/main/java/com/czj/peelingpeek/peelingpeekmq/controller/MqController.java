package com.czj.peelingpeek.peelingpeekmq.controller;

import com.alibaba.fastjson.JSON;
import com.czj.peelingpeek.peelingpeekmq.response.BaseResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: clownc
 * @Date: 2019-06-12 14:07
 */
@RestController
@Log4j2
public class MqController {
    @Qualifier("defaultProducer")
    @Autowired
    private DefaultMQProducer defaultMQProducer;//用defaultproducer发送消息
    @Value("${rocketmq.topic.getAccountTopic}")
    private String getAccountTopic;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @PostMapping("/getAccount")
    public BaseResponse getAccount(@RequestBody Object jsonData) throws InterruptedException {
        // Map<String, String[]> parameterMap = request.getParameterMap();//获取url里的请求参数
        //requestMsg.setParameterMap(parameterMap);
        try {
            String jsonString = JSON.toJSONString(jsonData);
            /*
             * Create a message instance, specifying topic, tag and message body.
             */
            Message msg = new Message(getAccountTopic /* Topic */,
                    "*" /* Tag */,
                    jsonString.getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
            );

            /*
             * Call send message to deliver message to one of brokers.
             */
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
    }
}
