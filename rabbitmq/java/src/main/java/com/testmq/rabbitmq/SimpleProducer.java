package com.testmq.rabbitmq;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * 简单的消息发送者演示
 */
public class SimpleProducer {
    public static void main(String[] args) throws Exception {
        Path runtimeDir = Paths.get("rabbitmq", "java", "runtime").toAbsolutePath();
        
        // 清空运行时目录
        ScenarioSupport.resetRuntime(runtimeDir);
        
        ProducerApp producer = new ProducerApp(runtimeDir);
        
        System.out.println("=== RabbitMQ 消息发送者演示 ===");
        System.out.println();
        
        // 场景 1：正常发送消息
        System.out.println("[场景 1] MQ 服务正常 - 发送消息到队列");
        System.out.println();
        for (int i = 1; i <= 5; i++) {
            Message msg = new Message(
                String.format("msg-%03d", i),
                i % 3 == 0 ? "critical" : "normal",
                "这是第 " + i + " 条消息的内容"
            );
            producer.send(msg);
        }
        System.out.println();
        System.out.println("状态：队列中的消息数 = " + ScenarioSupport.countLines(runtimeDir.resolve("queue.txt")));
        System.out.println();
        
        // 场景 2：MQ 故障，消息暂存到 outbox
        System.out.println("[场景 2] MQ 服务宕机 - 消息暂存到 outbox");
        System.out.println();
        producer.setMqUp(false);
        for (int i = 6; i <= 8; i++) {
            Message msg = new Message(
                String.format("msg-%03d", i),
                "critical",
                "这是第 " + i + " 条消息（MQ 宕机时）"
            );
            producer.send(msg);
        }
        System.out.println();
        System.out.println("状态：outbox 中的消息数 = " + ScenarioSupport.countLines(runtimeDir.resolve("outbox.txt")));
        System.out.println();
        
        // 场景 3：MQ 恢复，重放 outbox 消息
        System.out.println("[场景 3] MQ 服务恢复 - 重放 outbox 消息");
        System.out.println();
        producer.setMqUp(true);
        producer.replayOutbox();
        System.out.println();
        System.out.println("状态：队列中的消息数 = " + ScenarioSupport.countLines(runtimeDir.resolve("queue.txt")));
        System.out.println();
        
        // 显示队列内容
        System.out.println("=== 队列内容 ===");
        var messages = ScenarioSupport.readMessages(runtimeDir.resolve("queue.txt"));
        for (Message msg : messages) {
            System.out.printf("  [%s] 优先级：%s | 内容：%s%n", 
                msg.getMessageId(), msg.getPriority(), msg.getBody());
        }
        System.out.println();
        
        System.out.println("=== 发送者统计 ===");
        System.out.println("发送到队列的消息数：" + producer.getSentToQueue());
        System.out.println("暂存到 outbox 的消息数：" + producer.getSpooledToOutbox());
        System.out.println();
        
        System.out.println("按回车键继续到消费者演示...");
        new Scanner(System.in).nextLine();
    }
}
