CREATE DATABASE IF NOT EXISTS mq_demo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS mq_demo.processed_messages (
    message_id VARCHAR(128) PRIMARY KEY,
    priority VARCHAR(16) NOT NULL,
    body TEXT NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE USER IF NOT EXISTS 'mq_demo'@'%'
    IDENTIFIED BY 'mq_demo_dev_password';
CREATE USER IF NOT EXISTS 'mq_demo'@'localhost'
    IDENTIFIED BY 'mq_demo_dev_password';
ALTER USER 'mq_demo'@'%'
    IDENTIFIED WITH mysql_native_password BY 'mq_demo_dev_password';
ALTER USER 'mq_demo'@'localhost'
    IDENTIFIED WITH mysql_native_password BY 'mq_demo_dev_password';
GRANT SELECT, INSERT, UPDATE, CREATE ON mq_demo.* TO 'mq_demo'@'%';
GRANT SELECT, INSERT, UPDATE, CREATE ON mq_demo.* TO 'mq_demo'@'localhost';
FLUSH PRIVILEGES;
