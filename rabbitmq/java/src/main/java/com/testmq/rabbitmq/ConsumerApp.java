package com.testmq.rabbitmq;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConsumerApp {
    private final Path queueFile;
    private final Path pendingDbFile;
    private final Path deadLetterFile;
    private final Path dbFile;
    private boolean mysqlUp = true;
    private int persisted = 0;
    private int retried = 0;
    private int degraded = 0;
    private int pending = 0;

    public ConsumerApp(Path runtimeDir) {
        this.queueFile = runtimeDir.resolve("queue.txt");
        this.pendingDbFile = runtimeDir.resolve("pending_db.txt");
        this.deadLetterFile = runtimeDir.resolve("dead_letter.txt");
        this.dbFile = runtimeDir.resolve("db.txt");
    }

    public void setMysqlUp(boolean mysqlUp) {
        this.mysqlUp = mysqlUp;
    }

    public void processQueue(int backlogThreshold, int maxRetry) throws IOException {
        List<Message> messages = ScenarioSupport.readMessages(queueFile);
        if (messages.isEmpty()) {
            System.out.println("[consumer] queue empty");
            return;
        }

        int backlog = messages.size();
        System.out.println("[consumer] start consume, backlog=" + backlog);

        List<Message> keep = new ArrayList<>();
        if (backlog > backlogThreshold) {
            System.out.println("[consumer] backlog threshold exceeded, enter surge mode");
            for (Message message : messages) {
                if ("critical".equals(message.getPriority())) {
                    keep.add(message);
                } else if (keep.size() < backlogThreshold) {
                    keep.add(message);
                } else {
                    ScenarioSupport.appendLine(deadLetterFile, message.encode());
                    degraded++;
                    System.out.println("[consumer] degrade low priority message: " + message.getMessageId());
                }
            }
        } else {
            keep = messages;
        }

        ScenarioSupport.truncate(queueFile);
        for (Message message : keep) {
            persistWithRetry(message, maxRetry);
        }
    }

    public void flushPendingDb() throws IOException {
        List<Message> messages = ScenarioSupport.readMessages(pendingDbFile);
        if (messages.isEmpty()) {
            System.out.println("[consumer] pending_db empty");
            return;
        }
        if (!mysqlUp) {
            System.out.println("[consumer] mysql still down, skip pending_db flush");
            return;
        }

        System.out.println("[consumer] flush pending_db messages: " + messages.size());
        ScenarioSupport.truncate(pendingDbFile);
        for (Message message : messages) {
            ScenarioSupport.appendLine(dbFile, message.encode());
            persisted++;
        }
    }

    private void persistWithRetry(Message message, int maxRetry) throws IOException {
        if (mysqlUp) {
            ScenarioSupport.appendLine(dbFile, message.encode());
            persisted++;
            System.out.println("[consumer] persist to mysql: " + message.getMessageId());
            return;
        }

        for (int retry = 1; retry <= maxRetry; retry++) {
            retried++;
            System.out.println("[consumer] mysql down, retry " + retry + "/" + maxRetry + ": " + message.getMessageId());
        }

        ScenarioSupport.appendLine(pendingDbFile, message.encode());
        pending++;
        System.out.println("[consumer] write to pending_db: " + message.getMessageId());
    }

    public int getPersisted() {
        return persisted;
    }

    public int getRetried() {
        return retried;
    }

    public int getDegraded() {
        return degraded;
    }

    public int getPending() {
        return pending;
    }
}
