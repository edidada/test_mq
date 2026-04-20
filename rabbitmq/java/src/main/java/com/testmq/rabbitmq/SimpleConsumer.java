package com.testmq.rabbitmq;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * 简单的消息接收者演示
 */
public class SimpleConsumer {
    public static void main(String[] args) throws Exception {
        Path runtimeDir = Paths.get("rabbitmq", "java", "runtime").toAbsolutePath();
        
        ConsumerApp consumer = new ConsumerApp(runtimeDir);
        
        System.out.println("=== RabbitMQ 消息接收者演示 ===");
        System.out.println();
        
        // 显示当前队列状态
        int queueSize = ScenarioSupport.countLines(runtimeDir.resolve("queue.txt"));
        System.out.println("当前队列中的消息数：" + queueSize);
        System.out.println();
        
        // 场景 1：正常消费消息
        System.out.println("[场景 1] MySQL 正常 - 消费并持久化消息");
        System.out.println();
        consumer.processQueue(10, 2);
        System.out.println();
        System.out.println("状态：数据库中的记录数 = " + ScenarioSupport.countLines(runtimeDir.resolve("db.txt")));
        System.out.println();
        
        // 场景 2：MySQL 故障，消息暂存到 pending_db
        System.out.println("[场景 2] MySQL 宕机 - 消息暂存到 pending_db");
        System.out.println();
        consumer.setMysqlUp(false);
        
        // 先发送一些新消息到队列
        ProducerApp producer = new ProducerApp(runtimeDir);
        for (int i = 1; i <= 3; i++) {
            Message msg = new Message(
                String.format("new-msg-%03d", i),
                "normal",
                "MySQL 宕机时的新消息 " + i
            );
            producer.send(msg);
        }
        
        consumer.processQueue(10, 2);
        System.out.println();
        System.out.println("状态：pending_db 中的消息数 = " + ScenarioSupport.countLines(runtimeDir.resolve("pending_db.txt")));
        System.out.println();
        
        // 场景 3：MySQL 恢复，刷新 pending_db
        System.out.println("[场景 3] MySQL 恢复 - 刷新 pending_db 到数据库");
        System.out.println();
        consumer.setMysqlUp(true);
        consumer.flushPendingDb();
        System.out.println();
        System.out.println("状态：数据库中的记录数 = " + ScenarioSupport.countLines(runtimeDir.resolve("db.txt")));
        System.out.println();
        
        // 显示数据库内容
        System.out.println("=== 数据库内容 ===");
        var messages = ScenarioSupport.readMessages(runtimeDir.resolve("db.txt"));
        for (Message msg : messages) {
            System.out.printf("  [%s] 优先级：%s | 内容：%s%n", 
                msg.getMessageId(), msg.getPriority(), msg.getBody());
        }
        System.out.println();
        
        System.out.println("=== 消费者统计 ===");
        System.out.println("持久化到数据库的消息数：" + consumer.getPersisted());
        System.out.println("重试次数：" + consumer.getRetried());
        System.out.println("降级处理的消息数：" + consumer.getDegraded());
        System.out.println("写入 pending_db 的消息数：" + consumer.getPending());
        System.out.println();
    }
}
