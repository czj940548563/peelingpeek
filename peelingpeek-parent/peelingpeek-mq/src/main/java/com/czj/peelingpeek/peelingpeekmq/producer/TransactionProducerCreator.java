package com.czj.peelingpeek.peelingpeekmq.producer;


import com.czj.peelingpeek.peelingpeekmq.config.TransactionProducer;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @Author: clownc
 * @Date: 2019-06-03 14:46
 */
@Log4j2
@Configuration
public class TransactionProducerCreator {
    @Autowired
    private TransactionProducer transactionProducer;

    /**
     * 创建事务消息发送者实例
     *
     * @return
     * @throws MQClientException
     */
    @Bean
    public TransactionMQProducer transactionMQProducer() throws MQClientException {
        log.info(transactionProducer.toString());
        log.info("TransactionMQProducer 正在创建---------------------------------------");
        TransactionMQProducer producer = new TransactionMQProducer(transactionProducer.getGroupName());
        TransactionListenerImpl transactionListener = new TransactionListenerImpl();
        ExecutorService executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("client-transaction-msg-check-thread");
                return thread;
            }
        });
        producer.setNamesrvAddr(transactionProducer.getNamesrvAddr());
        producer.setExecutorService(executorService);
        producer.setTransactionListener(transactionListener);
        producer.start();
        log.info("TransactionMQProducer server开启成功---------------------------------.");
        return producer;
    }
}
