package com.mycompany.main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Appends message objects to one valid JSON array without extra libraries.
 */
public class MessageStore {
    private final Path jsonFile;

    public MessageStore(Path jsonFile) {
        this.jsonFile = Objects.requireNonNull(jsonFile, "jsonFile");
    }

    public void storeMessage(Message message) throws IOException {
        Objects.requireNonNull(message, "message");
        Path parent = jsonFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String existing = Files.exists(jsonFile)
                ? Files.readString(jsonFile, StandardCharsets.UTF_8).trim()
                : "[]";
        if (existing.isEmpty()) {
            existing = "[]";
        }
        if (!existing.startsWith("[") || !existing.endsWith("]")) {
            throw new IOException("The existing JSON file is not a JSON array.");
        }

        String object = toJsonObject(message);
        String contents;
        String arrayContents = existing.substring(1, existing.length() - 1)
                .trim();
        if (arrayContents.isEmpty()) {
            contents = "[\n  " + object + "\n]\n";
        } else {
            contents = "[\n  " + arrayContents + ",\n  " + object + "\n]\n";
        }
        Files.writeString(jsonFile, contents, StandardCharsets.UTF_8);
    }

    private String toJsonObject(Message message) {
        return "{"
                + "\"messageId\":\"" + escapeJson(message.getMessageId()) + "\","
                + "\"messageNumber\":" + message.getMessageNumber() + ","
                + "\"messageHash\":\"" + escapeJson(message.createMessageHash()) + "\","
                + "\"recipient\":\"" + escapeJson(message.getRecipient()) + "\","
                + "\"message\":\"" + escapeJson(message.getMessageText()) + "\","
                + "\"status\":\"" + escapeJson(message.getStatus()) + "\","
                + "\"createdAt\":\"" + message.getCreatedAt() + "\""
                + "}";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
