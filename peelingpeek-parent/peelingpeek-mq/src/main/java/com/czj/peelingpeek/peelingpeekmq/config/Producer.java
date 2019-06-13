package com.czj.peelingpeek.peelingpeekmq.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: clownc
 * @Date: 2019-05-31 16:04
 */
@Getter
@Setter
@Configuration
@ToString
public class Producer {
    @Value("${rocketmq.namesrvAddr}")
    private String namesrvAddr;
    @Value("${rocketmq.producer.groupName}")
    private String groupName;
}
