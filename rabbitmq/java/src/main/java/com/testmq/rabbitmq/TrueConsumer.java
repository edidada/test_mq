package com.testmq.rabbitmq;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 消费者示例
 */
public class TrueConsumer {
    private static final Logger logger = LogManager.getLogger(TrueConsumer.class);
    private static final String QUEUE_NAME = "test_queue";
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private CountDownLatch latch;
    private int messageCount = 0;

    public TrueConsumer() {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("app_user");
        factory.setPassword("123456");
    }

    public void connect() throws Exception {
        logger.info("connecting to RabbitMQ...");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        logger.info("connected successfully");
    }

    public void startConsuming(int expectedMessages) throws Exception {
        latch = new CountDownLatch(expectedMessages);
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("received: {}", message);
            
            // 模拟业务处理
            processMessage(message);
            
            // 手动 ACK
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            messageCount++;
            latch.countDown();
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
            logger.info("[consumer] consumption cancelled");
        });

        logger.info("waiting for messages...");
        latch.await(); // 等待所有消息处理完成
        logger.info("processed {} messages", messageCount);
    }

    private void processMessage(String message) {
        // 模拟业务逻辑处理
        String[] parts = message.split("\\|");
        if (parts.length == 3) {
            String messageId = parts[0];
            String priority = parts[1];
            String body = parts[2];
            logger.debug("  [processing] {} - priority: {}, body: {}", messageId, priority, body);
        }
    }

    public void close() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
            logger.info("connection closed");
        } catch (Exception e) {
            logger.error("error closing connection: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        TrueConsumer consumer = new TrueConsumer();
        try {
            consumer.connect();
            consumer.startConsuming(10); // 期望接收 10 条消息
            logger.info("done!");
        } catch (Exception e) {
            logger.error("error: {}", e.getMessage(), e);
        } finally {
            consumer.close();
        }
    }
}
