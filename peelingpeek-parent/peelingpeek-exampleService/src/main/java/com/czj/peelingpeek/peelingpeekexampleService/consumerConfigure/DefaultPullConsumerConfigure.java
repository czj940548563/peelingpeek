package com.czj.peelingpeek.peelingpeekexampleService.consumerConfigure;


import com.czj.peelingpeek.peelingpeekexampleService.config.PullConsumer;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.consumer.MQPullConsumer;
import org.apache.rocketmq.client.consumer.MQPullConsumerScheduleService;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Author: clownc
 * @Date: 2019-06-03 16:19
 */
@Configuration
@Log4j2
public abstract class DefaultPullConsumerConfigure {
    @Autowired
    private PullConsumer pullConsumer;

    // 开启消费者监听服务
    public void listener(String topic, String tag) throws MQClientException {
        log.info("开启" + topic + ":" + tag + "消费者-------------------");
        log.info(pullConsumer.toString());

        MQPullConsumerScheduleService ScheduleService = new MQPullConsumerScheduleService(pullConsumer.getGroupName());
        ScheduleService.getDefaultMQPullConsumer().setNamesrvAddr(pullConsumer.getNamesrvAddr());
        ScheduleService.setMessageModel(MessageModel.CLUSTERING);

        ScheduleService.registerPullTaskCallback(topic, (mq, context) -> {
            MQPullConsumer consumer = context.getPullConsumer();
            try {

                long offset = consumer.fetchConsumeOffset(mq, false);
                if (offset < 0)
                    offset = 0;

                PullResult pullResult = consumer.pull(mq, tag, offset, 32);
                //System.out.printf("%s%n", offset + "\t" + mq + "\t" + pullResult);
                switch (pullResult.getPullStatus()) {
                    case FOUND:
                        List<MessageExt> msgs = pullResult.getMsgFoundList();
                        DefaultPullConsumerConfigure.this.dealBody(msgs);
                        break;
                    case NO_MATCHED_MSG:
                        break;
                    case NO_NEW_MSG:
                    case OFFSET_ILLEGAL:
                        break;
                    default:
                        break;
                }
                consumer.updateConsumeOffset(mq, pullResult.getNextBeginOffset());
                //rocketmq默认更新消费者队列的offset的时间5s，这里为1毫秒，即每隔一毫秒拉取消息
                context.setPullNextDelayTimeMillis(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ScheduleService.start();

        log.info("rocketmq启动成功---------------------------------------");

    }

    // 处理body的业务
    public abstract void dealBody(List<MessageExt> msgs);
}
