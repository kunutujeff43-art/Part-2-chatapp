package com.mycompany.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the message arrays required by Part 3 for the current app session.
 * The arrays grow as real user actions and stored JSON provide new messages;
 * the application does not hard-code sample messages into them.
 */
public final class MessageArrays {
    private Message[] sentMessagesArray = new Message[0];
    private Message[] disregardedMessagesArray = new Message[0];
    private Message[] storedMessagesArray = new Message[0];
    private String[] messageHashesArray = new String[0];
    private String[] messageIdsArray = new String[0];

    public void recordCreatedMessage(Message message) {
        Objects.requireNonNull(message, "message");
        addIdentity(message);
    }

    public void recordSentMessage(Message message) {
        Objects.requireNonNull(message, "message");
        addMessageOnce(sentMessagesArray, message);
        addIdentity(message);
    }

    public void recordDisregardedMessage(Message message) {
        Objects.requireNonNull(message, "message");
        addMessageOnce(disregardedMessagesArray, message);
        addIdentity(message);
    }

    public void refreshStoredMessages(MessageStore store) throws IOException {
        Objects.requireNonNull(store, "store");
        storedMessagesArray =
                store.readStoredMessages().toArray(new Message[0]);
        for (Message message : storedMessagesArray) {
            addIdentity(message);
        }
    }

    public Message[] getSentMessagesArray() {
        return Arrays.copyOf(sentMessagesArray, sentMessagesArray.length);
    }

    public Message[] getDisregardedMessagesArray() {
        return Arrays.copyOf(disregardedMessagesArray,
                disregardedMessagesArray.length);
    }

    public Message[] getStoredMessagesArray() {
        return Arrays.copyOf(storedMessagesArray, storedMessagesArray.length);
    }

    public String[] getMessageHashesArray() {
        return Arrays.copyOf(messageHashesArray, messageHashesArray.length);
    }

    public String[] getMessageIdsArray() {
        return Arrays.copyOf(messageIdsArray, messageIdsArray.length);
    }

    public Optional<Message> findStoredMessageById(String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        return Arrays.stream(storedMessagesArray)
                .filter(message -> message.getMessageId()
                        .equals(messageId.trim()))
                .findFirst();
    }

    public Optional<Message> findMessageById(String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        String id = messageId.trim();
        for (Message message : sentMessagesArray) {
            if (message.getMessageId().equals(id)) {
                return Optional.of(message);
            }
        }
        for (Message message : storedMessagesArray) {
            if (message.getMessageId().equals(id)) {
                return Optional.of(message);
            }
        }
        for (Message message : disregardedMessagesArray) {
            if (message.getMessageId().equals(id)) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    public Optional<Message> findLongestStoredMessage() {
        return Arrays.stream(storedMessagesArray)
                .max((first, second) -> Integer.compare(
                        first.getMessageText().length(),
                        second.getMessageText().length()));
    }

    public List<Message> findSentOrStoredMessagesByRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return List.of();
        }
        String normalizedRecipient = recipient.trim()
                .toLowerCase(Locale.ROOT);
        Map<String, Message> matches = new LinkedHashMap<>();
        for (Message message : sentMessagesArray) {
            if (message.getRecipient().toLowerCase(Locale.ROOT)
                    .equals(normalizedRecipient)) {
                matches.put(message.getMessageId(), message);
            }
        }
        for (Message message : storedMessagesArray) {
            if (message.getRecipient().toLowerCase(Locale.ROOT)
                    .equals(normalizedRecipient)) {
                matches.put(message.getMessageId(), message);
            }
        }
        return new ArrayList<>(matches.values());
    }

    /**
     * Reports sent and stored messages together. Including both groups meets
     * the Part 3 report wording and the sample test's sent-message wording.
     */
    public String createMessageReport() {
        Map<String, Message> reportMessages = new LinkedHashMap<>();
        for (Message message : sentMessagesArray) {
            reportMessages.put(message.getMessageId(), message);
        }
        for (Message message : storedMessagesArray) {
            reportMessages.put(message.getMessageId(), message);
        }
        if (reportMessages.isEmpty()) {
            return "No sent or stored messages are available for the report.";
        }
        List<String> details = new ArrayList<>();
        for (Message message : reportMessages.values()) {
            details.add(message.formatFullDetails());
        }
        return String.join("\n\n", details);
    }

    private void addIdentity(Message message) {
        addUnique(messageIdsArray, message.getMessageId());
        addUnique(messageHashesArray, message.createMessageHash());
    }

    private void addMessageOnce(Message[] destination, Message message) {
        for (Message existing : destination) {
            if (existing.getMessageId().equals(message.getMessageId())) {
                return;
            }
        }
        Message[] expanded = Arrays.copyOf(destination, destination.length + 1);
        expanded[expanded.length - 1] = message;
        if (destination == sentMessagesArray) {
            sentMessagesArray = expanded;
        } else if (destination == disregardedMessagesArray) {
            disregardedMessagesArray = expanded;
        }
    }

    private void addUnique(String[] destination, String value) {
        for (String existing : destination) {
            if (existing.equals(value)) {
                return;
            }
        }
        String[] expanded = Arrays.copyOf(destination, destination.length + 1);
        expanded[expanded.length - 1] = value;
        if (destination == messageIdsArray) {
            messageIdsArray = expanded;
        } else if (destination == messageHashesArray) {
            messageHashesArray = expanded;
        }
    }
}
