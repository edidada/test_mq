package com.testmq.rabbitmq;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 生产者示例
 */
public class TrueProducer {
    private static final Logger logger = LogManager.getLogger(TrueProducer.class);
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
        logger.info("connecting to RabbitMQ...");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        logger.info("connected successfully");
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
        
        logger.info("sent: {} ({})", messageId, priority);
    }

    public void sendBatch(int count, int criticalEvery) throws Exception {
        logger.info("sending batch of {} messages", count);
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
            logger.info("connection closed");
        } catch (Exception e) {
            logger.error("error closing connection: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        TrueProducer producer = new TrueProducer();
        try {
            producer.connect();
            producer.sendBatch(10, 3); // 发送 10 条消息，每 3 条中有一条 critical
            logger.info("done!");
            Thread.sleep(1000);
        } catch (Exception e) {
            logger.error("error: {}", e.getMessage(), e);
        } finally {
            producer.close();
        }
    }
}
