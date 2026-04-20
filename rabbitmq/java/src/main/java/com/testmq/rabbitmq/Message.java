package com.testmq.rabbitmq;

public final class Message {
    private final String messageId;
    private final String priority;
    private final String body;

    public Message(String messageId, String priority, String body) {
        this.messageId = messageId;
        this.priority = priority;
        this.body = body;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getPriority() {
        return priority;
    }

    public String getBody() {
        return body;
    }

    public String encode() {
        return messageId + "|" + priority + "|" + body;
    }

    public static Message decode(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid message line: " + line);
        }
        return new Message(parts[0], parts[1], parts[2]);
    }
}
