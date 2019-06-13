package com.czj.peelingpeek.peelingpeekexampleService.config;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: clownc
 * @Date: 2019-05-31 16:47
 */
@Getter
@Setter
@Configuration
@ToString
public class PushConsumer {
    @Value("${rocketmq.namesrvAddr}")
    private String groupName;
    @Value("${rocketmq.consumer.groupName}")
    private String namesrvAddr;
}
