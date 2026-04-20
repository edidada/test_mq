package com.testmq.rabbitmq;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 生产者示例（支持配置文件）
 */
public class TrueProducer {
    private static final Logger logger = LogManager.getLogger(TrueProducer.class);
    private final ConfigLoader config;
    private final String queueName;
    private final int batchSize;
    private final int criticalEvery;
    private final String messagePrefix;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    public TrueProducer() {
        this.config = ConfigLoader.getInstance();
        
        // 从配置文件读取 RabbitMQ 连接参数
        String host = config.getString("rabbitmq.host", "localhost");
        int port = config.getInt("rabbitmq.port", 5672);
        String username = config.getString("rabbitmq.username", "app_user");
        String password = config.getString("rabbitmq.password", "123456");
        String vhost = config.getString("rabbitmq.vhost", "/");
        
        // 从配置文件读取队列参数
        this.queueName = config.getString("rabbitmq.queue.name", "test_queue");
        boolean durable = config.getBoolean("rabbitmq.queue.durable", true);
        
        // 从配置文件读取生产者参数
        this.batchSize = config.getInt("producer.batch.size", 10);
        this.criticalEvery = config.getInt("producer.batch.critical.every", 3);
        this.messagePrefix = config.getString("producer.message.prefix", "msg");
        
        logger.info("Initializing producer with config: host={}, port={}, queue={}, batchSize={}", 
                   host, port, queueName, batchSize);
        
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

    public void sendMessage(String messageId, String priority, String body) throws Exception {
        String message = messageId + "|" + priority + "|" + body;
        
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder()
            .contentType("text/plain")
            .deliveryMode(2); // persistent
        
        if ("critical".equals(priority)) {
            propsBuilder.priority(10);
        }
        
        channel.basicPublish("", queueName, propsBuilder.build(), 
                            message.getBytes(StandardCharsets.UTF_8));
        
        logger.info("sent: {} ({})", messageId, priority);
    }

    public void sendBatch(int count, int criticalEvery) throws Exception {
        logger.info("sending batch of {} messages", count);
        for (int i = 1; i <= count; i++) {
            String priority = (i % criticalEvery == 0) ? "critical" : "normal";
            String messageId = String.format("%s-%03d", messagePrefix, i);
            String body = "payload #" + i;
            sendMessage(messageId, priority, body);
        }
    }

    /**
     * 使用配置文件中定义的默认参数发送批次消息
     */
    public void sendDefaultBatch() throws Exception {
        sendBatch(batchSize, criticalEvery);
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
            
            // 使用配置文件中的默认参数发送消息
            producer.sendDefaultBatch();
            
            logger.info("done!");
            Thread.sleep(1000);
        } catch (Exception e) {
            logger.error("error: {}", e.getMessage(), e);
        } finally {
            producer.close();
        }
    }
}
