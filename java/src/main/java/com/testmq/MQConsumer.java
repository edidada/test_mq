package com.testmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MQConsumer {
    private static final Logger logger = LogManager.getLogger(MQConsumer.class);
    
    public static void main(String[] args) throws Exception {
        ConfigLoader config = ConfigLoader.getInstance();
        
        String namesrvAddr = config.getString("rocketmq.namesrv.addr", "127.0.0.1:9876");
        String consumerGroup = config.getString("rocketmq.consumer.group", "test_consumer_group");
        String topic = config.getString("rocketmq.topic.name", "TestTopic");
        int printEvery = config.getInt("consumer.print.every", 50);
        int processDelayMs = config.getInt("consumer.process.delay.ms", 10);
        
        logger.info("Initializing consumer with config: namesrvAddr={}, consumerGroup={}, topic={}", 
                   namesrvAddr, consumerGroup, topic);
        
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(topic, "*");

        AtomicInteger count = new AtomicInteger(0);
        
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    try {
                        int currentCount = count.incrementAndGet();
                        String body = new String(msg.getBody());
                        if (currentCount % printEvery == 0) {
                            logger.info("已消费 {} 条消息：{}", currentCount, body);
                        }
                        
                        Thread.sleep(processDelayMs);
                        
                    } catch (Exception e) {
                        logger.error("消费失败：{}", e.getMessage(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        logger.info("=== 消费者启动，等待消息... ===");
        consumer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("=== 消费者停止 ===");
            consumer.shutdown();
        }));
    }
}
