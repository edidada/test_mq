package com.testmq.rabbitmq;

import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 消费者示例
 */
public class TrueConsumer {
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
        System.out.println("[consumer] connecting to RabbitMQ...");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println("[consumer] connected successfully");
    }

    public void startConsuming(int expectedMessages) throws Exception {
        latch = new CountDownLatch(expectedMessages);
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[consumer] received: " + message);
            
            // 模拟业务处理
            processMessage(message);
            
            // 手动 ACK
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            messageCount++;
            latch.countDown();
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
            System.out.println("[consumer] consumption cancelled");
        });

        System.out.println("[consumer] waiting for messages...");
        latch.await(); // 等待所有消息处理完成
        System.out.println("[consumer] processed " + messageCount + " messages");
    }

    private void processMessage(String message) {
        // 模拟业务逻辑处理
        String[] parts = message.split("\\|");
        if (parts.length == 3) {
            String messageId = parts[0];
            String priority = parts[1];
            String body = parts[2];
            System.out.println("  [processing] " + messageId + " - priority: " + priority + ", body: " + body);
        }
    }

    public void close() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
            System.out.println("[consumer] connection closed");
        } catch (Exception e) {
            System.err.println("[consumer] error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TrueConsumer consumer = new TrueConsumer();
        try {
            consumer.connect();
            consumer.startConsuming(10); // 期望接收 10 条消息
            System.out.println("[consumer] done!");
        } catch (Exception e) {
            System.err.println("[consumer] error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
