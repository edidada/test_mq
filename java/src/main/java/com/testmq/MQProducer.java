package com.testmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class MQProducer {
    private static final Logger logger = LogManager.getLogger(MQProducer.class);
    
    public static void main(String[] args) throws Exception {
        ConfigLoader config = ConfigLoader.getInstance();
        
        String namesrvAddr = config.getString("rocketmq.namesrv.addr", "127.0.0.1:9876");
        String producerGroup = config.getString("rocketmq.producer.group", "test_producer_group");
        String topic = config.getString("rocketmq.topic.name", "TestTopic");
        String tags = config.getString("rocketmq.topic.tags", "TagA");
        int messageCount = config.getInt("producer.message.count", 1000);
        int printEvery = config.getInt("producer.batch.print.every", 100);
        String messagePrefix = config.getString("producer.message.prefix", "Hello RocketMQ");
        
        logger.info("Initializing producer with config: namesrvAddr={}, producerGroup={}, topic={}, messageCount={}", 
                   namesrvAddr, producerGroup, topic, messageCount);
        
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(namesrvAddr);
        producer.start();

        logger.info("=== 开始发送消息 ===");
        int failures = 0;
        for (int i = 1; i <= messageCount; i++) {
            try {
                Message msg = new Message(
                    topic,
                    tags,
                    (messagePrefix + " " + i).getBytes(RemotingHelper.DEFAULT_CHARSET)
                );
                SendResult sendResult = producer.send(msg);
                if (i % printEvery == 0) {
                    logger.info("已发送 {} 条消息", i);
                }
            } catch (Exception e) {
                failures++;
                logger.error("发送失败：{}", e.getMessage(), e);
            }
        }
        logger.info("=== 发送完成 ===");

        producer.shutdown();
        logger.info("Producer shutdown complete");
        if (failures > 0) {
            throw new IllegalStateException("Failed to send " + failures + " RocketMQ messages");
        }
    }
}
