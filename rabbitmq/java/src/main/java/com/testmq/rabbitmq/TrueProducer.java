package com.testmq.rabbitmq;

import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 生产者示例
 */
public class TrueProducer {
    private static final String QUEUE_NAME = "test_queue";
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    public TrueProducer() {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("app_user");
        factory.setPassword("123456");
    }

    public void connect() throws Exception {
        System.out.println("[producer] connecting to RabbitMQ...");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println("[producer] connected successfully");
    }

    public void sendMessage(String messageId, String priority, String body) throws Exception {
        String message = messageId + "|" + priority + "|" + body;
        
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder()
            .contentType("text/plain")
            .deliveryMode(2); // persistent
        
        if ("critical".equals(priority)) {
            propsBuilder.priority(10);
        }
        
        channel.basicPublish("", QUEUE_NAME, propsBuilder.build(), 
                            message.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("[producer] sent: " + messageId + " (" + priority + ")");
    }

    public void sendBatch(int count, int criticalEvery) throws Exception {
        System.out.println("[producer] sending batch of " + count + " messages");
        for (int i = 1; i <= count; i++) {
            String priority = (i % criticalEvery == 0) ? "critical" : "normal";
            String messageId = String.format("msg-%03d", i);
            String body = "payload #" + i;
            sendMessage(messageId, priority, body);
        }
    }

    public void close() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
            System.out.println("[producer] connection closed");
        } catch (Exception e) {
            System.err.println("[producer] error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TrueProducer producer = new TrueProducer();
        try {
            producer.connect();
            producer.sendBatch(10, 3); // 发送 10 条消息，每 3 条中有一条 critical
            System.out.println("[producer] done!");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("[producer] error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}
