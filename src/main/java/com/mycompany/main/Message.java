package com.mycompany.main;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * One message in the console messaging application.
 */
public class Message {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> GENERATED_IDS = new HashSet<>();
    private static final List<Message> SENT_MESSAGES = new ArrayList<>();
    private static int totalMessagesSent;

    private static final Pattern SOUTH_AFRICAN_INTERNATIONAL_NUMBER =
            Pattern.compile("^\\+27\\d{9}$");

    private final String messageId;
    private final int messageNumber;
    private final String recipient;
    private final String messageText;
    private final Instant createdAt;
    private String status = "DRAFT";
    private String messageHash;

    public Message(String recipient, String messageText, int messageNumber) {
        this(generateUniqueMessageId(), recipient, messageText, messageNumber);
    }

    /**
     * This constructor is useful for predictable unit tests.
     */
    public Message(String messageId, String recipient, String messageText,
                   int messageNumber) {
        this.messageId = Objects.requireNonNullElse(messageId, "");
        this.recipient = Objects.requireNonNullElse(recipient, "");
        this.messageText = Objects.requireNonNullElse(messageText, "");
        this.messageNumber = messageNumber;
        this.createdAt = Instant.now();
    }

    private static synchronized String generateUniqueMessageId() {
        String candidate;
        do {
            long randomNumber =
                    RANDOM.nextLong(1_000_000_000L, 10_000_000_000L);
            String generatedNumber = Long.toString(randomNumber);
            candidate = generatedNumber.substring(0, 10);
        } while (!GENERATED_IDS.add(candidate));
        return candidate;
    }

    /**
     * Confirms that the message ID is exactly ten numeric characters.
     */
    public boolean checkMessageID() {
        return messageId.matches("\\d{10}");
    }

    /**
     * Uses the same +27 international code.
     */
    public String checkRecipientCell() {
        if (SOUTH_AFRICAN_INTERNATIONAL_NUMBER.matcher(recipient).matches()) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not "
                + "contain an international code. Please correct the number "
                + "and try again.";
        
    }
public static boolean isRecipientCellValid(String recipient) {
    return recipient != null
            && SOUTH_AFRICAN_INTERNATIONAL_NUMBER.matcher(recipient).matches();
}
    /**
     * returns the rubric's ready-to-send or over 250 character result.
     */
    public String checkMessageLength() {
        if (messageText.length() <= 250) {
            return "Message ready to send.";
        }
        int excessCharacters = messageText.length() - 250;
        return "Message exceeds 250 characters by " + excessCharacters
                + "; please reduce the size.";
    }

    /**
     * Hash format: first two ID digits, message number, first and last words.
     */
    public String createMessageHash() {
        if (messageHash != null) {
            return messageHash;
        }

        String[] words = messageText.trim().split("[^\\p{L}\\p{N}]+");
        List<String> messageWords = new ArrayList<>();
        for (String word : words) {
            if (!word.isBlank()) {
                messageWords.add(word);
            }
        }

        String firstWord = messageWords.isEmpty()
                ? "" : messageWords.get(0);
        String lastWord = messageWords.isEmpty()
                ? "" : messageWords.get(messageWords.size() - 1);

        String idPrefix = checkMessageID() ? messageId.substring(0, 2) : "00";
        messageHash = (idPrefix + ":" + messageNumber + ":"
                + firstWord + lastWord).toUpperCase();
        return messageHash;
    }

    /**
     * Performs the selected SEND, STORE, or DISCARD action.
     */
    public String SentMessage(String selectedAction, Path jsonFile) {
        if (selectedAction == null) {
            return "Please select Send, Disregard, or Store.";
        }

        switch (selectedAction.trim().toUpperCase()) {
            case "SEND":
                String lengthResult = checkMessageLength();
                if (!"Message ready to send.".equals(lengthResult)) {
                    return lengthResult;
                }
                if (!checkRecipientCell().startsWith(
                        "Cell phone number successfully captured.")) {
                    return checkRecipientCell();
                }
                markAsSent();
                return "Message successfully sent.";
            case "DISCARD":
                return "Press 0 to delete the message.";
            case "STORE":
                if (jsonFile == null) {
                    return "Message could not be stored: JSON file path is missing.";
                }
                try {
                    storeMessage(jsonFile);
                    return "Message successfully stored.";
                } catch (java.io.IOException exception) {
                    return "Message could not be stored: " + exception.getMessage();
                }
            default:
                return "Invalid message action. Choose Send, Disregard, or Store.";
        }
    }

    private synchronized void markAsSent() {
        if (!"SENT".equals(status)) {
            status = "SENT";
            SENT_MESSAGES.add(this);
            totalMessagesSent++;
        }
    }

    /**
     * Stores this message in a valid JSON array file.
     */
    public void storeMessage(Path jsonFile) throws java.io.IOException {
        String previousStatus = status;
        status = "STORED";
        try {
            new MessageStore(jsonFile).storeMessage(this);
        } catch (java.io.IOException exception) {
            status = previousStatus;
            throw exception;
        }
    }

    /**
     * Returns sent-message details in the rubric's required order.
     */
    public String formatDetails() {
        return "Message ID: " + messageId
                + "\nMessage Hash: " + createMessageHash()
                + "\nRecipient: " + recipient
                + "\nMessage: " + messageText;
    }

    /**
     * Returns all messages sent during this run.
     */
    public String printMessages() {
        if (SENT_MESSAGES.isEmpty()) {
            return "No messages have been sent yet.";
        }
        List<String> details = new ArrayList<>();
        for (Message sentMessage : SENT_MESSAGES) {
            details.add(sentMessage.formatDetails());
        }
        return String.join("\n\n", details);
    }

    public int returnTotalMessages() {
        return totalMessagesSent;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    static List<Message> getSentMessagesForTest() {
        return Collections.unmodifiableList(SENT_MESSAGES);
    }

    static void resetSentMessagesForTest() {
        SENT_MESSAGES.clear();
        totalMessagesSent = 0;
    }
}
