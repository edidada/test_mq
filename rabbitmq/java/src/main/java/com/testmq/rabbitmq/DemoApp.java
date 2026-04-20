package com.testmq.rabbitmq;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DemoApp {
    public static void main(String[] args) throws Exception {
        Path runtimeDir = Paths.get("rabbitmq", "java", "runtime").toAbsolutePath();
        ScenarioSupport.resetRuntime(runtimeDir);

        ProducerApp producer = new ProducerApp(runtimeDir);
        ConsumerApp consumer = new ConsumerApp(runtimeDir);

        System.out.println("=== Java RabbitMQ resilience demo ===");

        System.out.println();
        System.out.println("[scenario-1] backlog");
        producer.sendBatch(ScenarioSupport.buildMessages("backlog", 10, 4));
        System.out.println("[state] queue backlog before consume: " + ScenarioSupport.countLines(runtimeDir.resolve("queue.txt")));
        consumer.processQueue(6, 2);
        System.out.println("[state] queue backlog after consume: " + ScenarioSupport.countLines(runtimeDir.resolve("queue.txt")));

        System.out.println();
        System.out.println("[scenario-2] mq down");
        producer.setMqUp(false);
        producer.sendBatch(ScenarioSupport.buildMessages("mq-down", 4, 2));
        System.out.println("[state] outbox size while mq down: " + ScenarioSupport.countLines(runtimeDir.resolve("outbox.txt")));
        producer.setMqUp(true);
        producer.replayOutbox();
        System.out.println("[state] queue size after mq recovery: " + ScenarioSupport.countLines(runtimeDir.resolve("queue.txt")));

        System.out.println();
        System.out.println("[scenario-3] mysql down");
        consumer.setMysqlUp(false);
        consumer.processQueue(20, 2);
        System.out.println("[state] pending_db size while mysql down: " + ScenarioSupport.countLines(runtimeDir.resolve("pending_db.txt")));
        consumer.setMysqlUp(true);
        consumer.flushPendingDb();
        System.out.println("[state] db size after mysql recovery: " + ScenarioSupport.countLines(runtimeDir.resolve("db.txt")));

        System.out.println();
        System.out.println("[scenario-4] mq and mysql both down");
        producer.setMqUp(false);
        consumer.setMysqlUp(false);
        producer.sendBatch(ScenarioSupport.buildMessages("double-down", 3, 1));
        System.out.println("[state] outbox size during double-down: " + ScenarioSupport.countLines(runtimeDir.resolve("outbox.txt")));
        producer.setMqUp(true);
        producer.replayOutbox();
        consumer.processQueue(20, 2);
        System.out.println("[state] pending_db size after double-down consume: " + ScenarioSupport.countLines(runtimeDir.resolve("pending_db.txt")));
        consumer.setMysqlUp(true);
        consumer.flushPendingDb();

        System.out.println();
        System.out.println("=== Summary ===");
        System.out.println("persisted=" + consumer.getPersisted());
        System.out.println("retried=" + consumer.getRetried());
        System.out.println("degraded=" + consumer.getDegraded());
        System.out.println("pending_written=" + consumer.getPending());
        System.out.println("db_records=" + ScenarioSupport.countLines(runtimeDir.resolve("db.txt")));
        System.out.println("dead_letters=" + ScenarioSupport.countLines(runtimeDir.resolve("dead_letter.txt")));
        System.out.println("outbox_remaining=" + ScenarioSupport.countLines(runtimeDir.resolve("outbox.txt")));
        System.out.println("pending_remaining=" + ScenarioSupport.countLines(runtimeDir.resolve("pending_db.txt")));
    }
}
