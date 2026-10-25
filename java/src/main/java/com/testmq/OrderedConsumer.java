package com.testmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * RocketMQ 顺序消息 —— 消费者端。
 *
 * 关键：使用 MessageListenerOrderly（顺序监听），RocketMQ 会保证
 * 同一个 MessageQueue 内的消息被“单线程、按顺序”消费。
 * 与之相对的是 MessageListenerConcurrently（并发消费，不保证顺序）。
 */
public class OrderedConsumer {
    private static final Logger logger = LogManager.getLogger(OrderedConsumer.class);

    public static void main(String[] args) throws Exception {
        String namesrv = System.getenv().getOrDefault("ROCKETMQ_NAMESRV", "127.0.0.1:9876");
        String topic = System.getenv().getOrDefault("ROCKETMQ_TOPIC", "OrderTopic");
        String group = System.getenv().getOrDefault("ROCKETMQ_CONSUMER_GROUP", "ordered_consumer_group");

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(namesrv);
        consumer.subscribe(topic, "*");
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    logger.info("消费 topic={} queue={} body={}",
                            msg.getTopic(), msg.getQueueId(), new String(msg.getBody()));
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        consumer.start();
        logger.info("=== RocketMQ 顺序消费者已启动 ===");
    }
}
