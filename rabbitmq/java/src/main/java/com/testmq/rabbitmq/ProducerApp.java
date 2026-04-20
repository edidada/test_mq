package com.testmq.rabbitmq;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ProducerApp {
    private final Path queueFile;
    private final Path outboxFile;
    private boolean mqUp = true;
    private int sentToQueue = 0;
    private int spooledToOutbox = 0;

    public ProducerApp(Path runtimeDir) {
        this.queueFile = runtimeDir.resolve("queue.txt");
        this.outboxFile = runtimeDir.resolve("outbox.txt");
    }

    public void setMqUp(boolean mqUp) {
        this.mqUp = mqUp;
    }

    public void send(Message message) throws IOException {
        Path target = mqUp ? queueFile : outboxFile;
        ScenarioSupport.appendLine(target, message.encode());
        if (mqUp) {
            sentToQueue++;
            System.out.println("[producer] send to queue: " + message.getMessageId() + " (" + message.getPriority() + ")");
        } else {
            spooledToOutbox++;
            System.out.println("[producer] mq down, spool to outbox: " + message.getMessageId());
        }
    }

    public void sendBatch(List<Message> messages) throws IOException {
        for (Message message : messages) {
            send(message);
        }
    }

    public void replayOutbox() throws IOException {
        if (!mqUp) {
            System.out.println("[producer] mq still down, skip outbox replay");
            return;
        }

        List<Message> messages = ScenarioSupport.readMessages(outboxFile);
        if (messages.isEmpty()) {
            System.out.println("[producer] outbox empty, nothing to replay");
            return;
        }

        System.out.println("[producer] replay outbox messages: " + messages.size());
        for (Message message : messages) {
            ScenarioSupport.appendLine(queueFile, message.encode());
            sentToQueue++;
        }
        ScenarioSupport.truncate(outboxFile);
    }

    public int getSentToQueue() {
        return sentToQueue;
    }

    public int getSpooledToOutbox() {
        return spooledToOutbox;
    }
}
