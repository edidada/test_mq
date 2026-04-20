package com.testmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 配置测试类
 */
public class ConfigTest {
    private static final Logger logger = LogManager.getLogger(ConfigTest.class);
    
    public static void main(String[] args) {
        try {
            ConfigLoader config = ConfigLoader.getInstance();
            
            logger.info("=== 测试配置加载 ===");
            logger.info("RocketMQ NameServer 地址：{}", config.getString("rocketmq.namesrv.addr"));
            logger.info("生产者组：{}", config.getString("rocketmq.producer.group"));
            logger.info("消费者组：{}", config.getString("rocketmq.consumer.group"));
            logger.info("主题：{}", config.getString("rocketmq.topic.name"));
            logger.info("消息数量：{}", config.getInt("producer.message.count"));
            logger.info("打印间隔：{}", config.getInt("producer.batch.print.every"));
            logger.info("消息前缀：{}", config.getString("producer.message.prefix"));
            logger.info("=== 配置加载成功 ===");
            
        } catch (Exception e) {
            logger.error("配置加载失败：{}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
