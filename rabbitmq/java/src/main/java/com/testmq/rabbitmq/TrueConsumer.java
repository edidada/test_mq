package com.testmq.rabbitmq;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 消费者示例（支持配置文件）
 */
public class TrueConsumer {
    private static final Logger logger = LogManager.getLogger(TrueConsumer.class);
    private final ConfigLoader config;
    private final String queueName;
    private final int prefetchCount;
    private final int expectedMessages;
    private final boolean autoAck;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private CountDownLatch latch;
    private int messageCount = 0;

    public TrueConsumer() {
        this.config = ConfigLoader.getInstance();
        
        // 从配置文件读取 RabbitMQ 连接参数
        String host = config.getString("rabbitmq.host", "localhost");
        int port = config.getInt("rabbitmq.port", 5672);
        String username = config.getString("rabbitmq.username", "app_user");
        String password = config.getString("rabbitmq.password", "123456");
        String vhost = config.getString("rabbitmq.vhost", "/");
        
        // 从配置文件读取队列参数
        this.queueName = config.getString("rabbitmq.queue.name", "test_queue");
        
        // 从配置文件读取消费者参数
        this.prefetchCount = config.getInt("consumer.prefetch.count", 1);
        this.expectedMessages = config.getInt("consumer.expected.messages", 10);
        this.autoAck = config.getBoolean("consumer.auto.ack", false);
        
        logger.info("Initializing consumer with config: host={}, port={}, queue={}, prefetch={}", 
                   host, port, queueName, prefetchCount);
        
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(vhost);
    }

    public void connect() throws Exception {
        logger.info("connecting to RabbitMQ...");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        logger.info("connected successfully");
    }

    public void startConsuming(int expectedMessages) throws Exception {
        latch = new CountDownLatch(expectedMessages);
        
        // 设置 QoS - 每次只处理一条消息
        channel.basicQos(prefetchCount);
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("received: {}", message);
            
            // 模拟业务处理
            processMessage(message);
            
            // 手动 ACK
            if (!autoAck) {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            messageCount++;
            latch.countDown();
        };
        
        channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {
            logger.info("[consumer] consumption cancelled");
        });

        logger.info("waiting for messages...");
        latch.await(); // 等待所有消息处理完成
        logger.info("processed {} messages", messageCount);
    }

    /**
     * 使用配置文件中的默认参数开始消费
     */
    public void startDefaultConsuming() throws Exception {
        startConsuming(expectedMessages);
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
            
            // 使用配置文件中的默认参数消费消息
            consumer.startDefaultConsuming();
            
            logger.info("done!");
        } catch (Exception e) {
            logger.error("error: {}", e.getMessage(), e);
        } finally {
            consumer.close();
        }
    }
}
