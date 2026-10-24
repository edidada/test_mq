package com.testmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置加载器 - 从 application.properties 读取配置
 * 
 * 使用示例：
 * <pre>
 * ConfigLoader config = ConfigLoader.getInstance();
 * String namesrvAddr = config.getString("rocketmq.namesrv.addr", "127.0.0.1:9876");
 * int messageCount = config.getInt("producer.message.count", 1000);
 * </pre>
 */
public class ConfigLoader {
    private static final Logger logger = LogManager.getLogger(ConfigLoader.class);
    private static volatile ConfigLoader instance;
    private final Properties properties;

    private ConfigLoader() {
        properties = new Properties();
        loadProperties();
    }

    /**
     * 获取单例实例
     * @return ConfigLoader 实例
     */
    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    /**
     * 加载配置文件
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Unable to find application.properties");
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
            logger.info("Configuration loaded successfully from application.properties");
        } catch (IOException ex) {
            logger.error("Exception while loading properties: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to load application.properties", ex);
        }
    }

    /**
     * 获取字符串配置
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue.trim();
        }
        String environmentValue = System.getenv(key.toUpperCase().replace('.', '_').replace('-', '_'));
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取字符串配置（无默认值）
     * @param key 配置键
     * @return 配置值
     */
    public String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Required property '" + key + "' not found");
        }
        return value;
    }

    /**
     * 获取整数配置
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for '{}': '{}', using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取整数配置（无默认值）
     * @param key 配置键
     * @return 配置值
     */
    public int getInt(String key) {
        String value = getString(key, null);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Required property '" + key + "' not found");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.error("Invalid integer value for '{}': '{}'", key, value);
            throw new RuntimeException("Invalid integer value for property: " + key, e);
        }
    }

    /**
     * 获取布尔配置
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 获取布尔配置（无默认值）
     * @param key 配置键
     * @return 配置值
     */
    public boolean getBoolean(String key) {
        String value = getString(key, null);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Required property '" + key + "' not found");
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 获取所有配置（用于调试）
     * @return Properties 对象
     */
    public Properties getAllProperties() {
        return properties;
    }

    /**
     * 打印所有配置（用于调试）
     */
    public void printAllProperties() {
        logger.info("=== Application Configuration ===");
        properties.forEach((key, value) -> {
            // 不打印敏感信息
            String keyStr = key.toString();
            if (!keyStr.toLowerCase().contains("password") && 
                !keyStr.toLowerCase().contains("secret")) {
                logger.info("{} = {}", key, value);
            } else {
                logger.info("{} = ****", key);
            }
        });
        logger.info("================================");
    }
}
