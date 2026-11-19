package com.mycompany.main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
        List<Message> messages = readStoredMessages();
        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index).getMessageId().equals(
                    message.getMessageId())) {
                messages.set(index, message);
                writeStoredMessages(messages);
                return;
            }
        }
        messages.add(message);
        writeStoredMessages(messages);
    }

    public List<Message> readStoredMessages() throws IOException {
        if (!Files.exists(jsonFile)) {
            return new ArrayList<>();
        }

        String contents = Files.readString(jsonFile, StandardCharsets.UTF_8);
        if (contents.isBlank()) {
            return new ArrayList<>();
        }

        final Object parsed;
        try {
            parsed = new JsonParser(contents).parseDocument();
        } catch (IllegalArgumentException exception) {
            throw new IOException("The stored messages JSON is invalid.",
                    exception);
        }
        if (!(parsed instanceof List<?> records)) {
            throw new IOException("The stored messages JSON must be an array.");
        }

        List<Message> messages = new ArrayList<>();
        for (Object record : records) {
            if (!(record instanceof Map<?, ?> fields)) {
                throw new IOException(
                        "Each stored message in the JSON array must be an object.");
            }
            try {
                String messageId = stringField(fields, "messageId", "");
                String sender = stringField(fields, "sender", "Unknown");
                String recipient = stringField(fields, "recipient", "");
                String messageText = stringField(fields, "message", "");
                int messageNumber = intField(fields.get("messageNumber"),
                        messages.size());
                String status = stringField(fields, "status", "STORED");
                String messageHash = stringField(fields, "messageHash", null);
                String createdAtText = stringField(fields, "createdAt", null);
                Instant createdAt = createdAtText == null
                        ? Instant.EPOCH : Instant.parse(createdAtText);
                messages.add(Message.restore(messageId, sender, recipient,
                        messageText, messageNumber, status, messageHash,
                        createdAt));
            } catch (DateTimeParseException | IllegalArgumentException exception) {
                throw new IOException("A stored message has invalid data.",
                        exception);
            }
        }
        return messages;
    }

    public Optional<Message> deleteStoredMessageByHash(String messageHash)
            throws IOException {
        if (messageHash == null || messageHash.isBlank()) {
            return Optional.empty();
        }
        List<Message> messages = readStoredMessages();
        for (int index = 0; index < messages.size(); index++) {
            Message message = messages.get(index);
            if (message.createMessageHash().equalsIgnoreCase(
                    messageHash.trim())) {
                messages.remove(index);
                writeStoredMessages(messages);
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    private void writeStoredMessages(List<Message> messages) throws IOException {
        Path absoluteFile = jsonFile.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder contents = new StringBuilder("[\n");
        for (int index = 0; index < messages.size(); index++) {
            contents.append("  ").append(toJsonObject(messages.get(index)));
            if (index + 1 < messages.size()) {
                contents.append(',');
            }
            contents.append('\n');
        }
        contents.append("]\n");

        Path temporaryFile = Files.createTempFile(parent, "messages-", ".json");
        try {
            Files.writeString(temporaryFile, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, absoluteFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, absoluteFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private String stringField(Map<?, ?> fields, String key, String fallback) {
        Object value = fields.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int intField(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(String.valueOf(value)).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "messageNumber must be a whole number.", exception);
        }
    }

    private String toJsonObject(Message message) {
        return "{"
                + "\"messageId\":\"" + escapeJson(message.getMessageId()) + "\","
                + "\"messageNumber\":" + message.getMessageNumber() + ","
                + "\"messageHash\":\"" + escapeJson(message.createMessageHash()) + "\","
                + "\"sender\":\"" + escapeJson(message.getSender()) + "\","
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

    private static final class JsonParser {
        private final String input;
        private int position;

        private JsonParser(String input) {
            this.input = input;
        }

        private Object parseDocument() {
            Object value = parseValue();
            skipWhitespace();
            if (position != input.length()) {
                throw error("Unexpected characters after the JSON value.");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (position >= input.length()) {
                throw error("Unexpected end of JSON.");
            }
            return switch (input.charAt(position)) {
                case '[' -> parseArray();
                case '{' -> parseObject();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (take(']')) {
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (take(']')) {
                    return values;
                }
                expect(',');
            }
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> values = new LinkedHashMap<>();
            skipWhitespace();
            if (take('}')) {
                return values;
            }
            while (true) {
                skipWhitespace();
                if (position >= input.length()
                        || input.charAt(position) != '"') {
                    throw error("JSON object keys must be strings.");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                values.put(key, parseValue());
                skipWhitespace();
                if (take('}')) {
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char character = input.charAt(position++);
                if (character == '"') {
                    return value.toString();
                }
                if (character < 0x20) {
                    throw error("Unescaped control character in JSON string.");
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (position >= input.length()) {
                    throw error("Incomplete JSON escape sequence.");
                }
                char escaped = input.charAt(position++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("Invalid JSON escape sequence.");
                }
            }
            throw error("Unterminated JSON string.");
        }

        private char parseUnicodeEscape() {
            if (position + 4 > input.length()) {
                throw error("Incomplete Unicode escape sequence.");
            }
            String hex = input.substring(position, position + 4);
            try {
                char value = (char) Integer.parseInt(hex, 16);
                position += 4;
                return value;
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape sequence.");
            }
        }

        private Object parseNumber() {
            int start = position;
            take('-');
            if (take('0')) {
                if (position < input.length()
                        && Character.isDigit(input.charAt(position))) {
                    throw error("Leading zero in JSON number.");
                }
            } else {
                requireDigits();
            }
            if (take('.')) {
                requireDigits();
            }
            if (take('e') || take('E')) {
                if (!take('+')) {
                    take('-');
                }
                requireDigits();
            }
            if (start == position) {
                throw error("Invalid JSON value.");
            }
            try {
                return new BigDecimal(input.substring(start, position));
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON number.");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, position)) {
                throw error("Invalid JSON value.");
            }
            position += literal.length();
            return value;
        }

        private void requireDigits() {
            int start = position;
            while (position < input.length()
                    && Character.isDigit(input.charAt(position))) {
                position++;
            }
            if (start == position) {
                throw error("Expected a digit in JSON number.");
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (!take(expected)) {
                throw error("Expected '" + expected + "'.");
            }
        }

        private boolean take(char expected) {
            if (position < input.length()
                    && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length()
                    && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    message + " At character " + position + ".");
        }
    }
}
