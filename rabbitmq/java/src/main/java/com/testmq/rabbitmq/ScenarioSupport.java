package com.testmq.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScenarioSupport {
    private ScenarioSupport() {
    }

    public static List<Message> buildMessages(String prefix, int count, int criticalEvery) {
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String priority = criticalEvery > 0 && i % criticalEvery == 0 ? "critical" : "normal";
            String messageId = String.format("%s-%03d", prefix, i);
            String body = prefix + " payload #" + i;
            messages.add(new Message(messageId, priority, body));
        }
        return messages;
    }

    public static void resetRuntime(Path runtimeDir) throws IOException {
        if (Files.exists(runtimeDir)) {
            Files.walk(runtimeDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
        Files.createDirectories(runtimeDir);
    }

    public static void appendLine(Path path, String line) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(
            path,
            (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public static List<Message> readMessages(Path path) throws IOException {
        List<Message> messages = new ArrayList<>();
        if (!Files.exists(path)) {
            return messages;
        }
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                messages.add(Message.decode(line));
            }
        }
        return messages;
    }

    public static int countLines(Path path) throws IOException {
        if (!Files.exists(path)) {
            return 0;
        }
        int count = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static void truncate(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
