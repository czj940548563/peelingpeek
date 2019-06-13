package com.czj.peelingpeek.peelingpeekexampleService.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: clownc
 * @Date: 2019-06-03 16:25
 */
@Getter
@Setter
@Configuration
@ToString
public class PullConsumer {
    @Value("${rocketmq.namesrvAddr}")
    private String namesrvAddr;
    @Value("${rocketmq.pullConsumer.groupName}")
    private String groupName;
}
