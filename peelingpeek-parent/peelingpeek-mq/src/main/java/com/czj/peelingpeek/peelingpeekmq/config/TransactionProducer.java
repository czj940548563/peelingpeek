package com.czj.peelingpeek.peelingpeekmq.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: clownc
 * @Date: 2019-06-03 15:30
 */
@Getter
@Setter
@Configuration
@ToString
public class TransactionProducer {
    @Value("${rocketmq.namesrvAddr}")
    private String namesrvAddr;
    @Value("${rocketmq.transactionProducer.groupName}")
    private String groupName;
}

