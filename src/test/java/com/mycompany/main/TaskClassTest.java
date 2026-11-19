package com.mycompany.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class TaskClassTest {
    @TempDir
    Path temporaryDirectory;

    @AfterEach
    void resetMessageCounters() {
        Message.resetSentMessagesForTest();
    }

    @Test
    void messageIdMustContainExactlyTenDigits() {
        assertTrue(new Message("0012345678", "+27718693002",
                "Hi Mike, can you join us for dinner tonight?", 0)
                .checkMessageID());
        assertFalse(new Message("12345", "+27718693002", "Hello", 0)
                .checkMessageID());
    }

    @Test
    void recipientMustUseThePartOneSouthAfricanInternationalFormat() {
        Message valid = new Message("0012345678", "+27718693002", "Hello", 0);
        Message localFormat = new Message("0012345679", "08575975889",
                "Hello", 1);

        assertEquals("Cell phone number successfully captured.",
                valid.checkRecipientCell());
        assertTrue(localFormat.checkRecipientCell()
                .startsWith("Cell phone number is incorrectly formatted"));
        assertFalse(Message.isRecipientCellValid("+2771869300"));
        assertFalse(Message.isRecipientCellValid("+277186930021"));
    }

    @Test
    void messageLengthAccepts250AndRejectsMoreThan250Characters() {
        Message valid = new Message("0012345678", "+27718693002",
                "a".repeat(250), 0);
        Message tooLong = new Message("0012345679", "+27718693002",
                "a".repeat(251), 1);

        assertEquals("Message ready to send.", valid.checkMessageLength());
        assertEquals("Message exceeds 250 characters by 1; please reduce "
                + "the size.", tooLong.checkMessageLength());
    }

    @Test
    void hashUsesIdPrefixMessageNumberAndFirstAndLastWords() {
        Message message = new Message("0012345678", "+27718693002",
                "Hi Mike, can you join us for dinner tonight?", 0);

        assertEquals("00:0:HITONIGHT", message.createMessageHash());
    }

    @Test
    void sendStoresDetailsAndIncrementsTotalOnlyOnce() {
        Message message = new Message("0012345678", "+27718693002",
                "Hi Mike, can you join us for dinner tonight?", 0);

        assertEquals("Message successfully sent.",
                message.SentMessage("SEND",
                        temporaryDirectory.resolve("messages.json")));
        assertEquals(1, message.returnTotalMessages());
        assertEquals(1, Message.getSentMessagesForTest().size());
        assertTrue(message.printMessages()
                .contains("Message Hash: 00:0:HITONIGHT"));

        message.SentMessage("SEND",
                temporaryDirectory.resolve("messages.json"));
        assertEquals(1, message.returnTotalMessages());
    }

    @Test
    void invalidRecipientCannotBeSentButCanBeDiscarded() {
        Message message = new Message("0012345678", "08575975889",
                "Hi Keegan, did you receive the payment?", 1);

        assertTrue(message.SentMessage("SEND",
                temporaryDirectory.resolve("messages.json"))
                .startsWith("Cell phone number is incorrectly formatted"));
        assertEquals("Press 0 to delete the message.",
                message.SentMessage("DISCARD",
                        temporaryDirectory.resolve("messages.json")));
        assertEquals(0, message.returnTotalMessages());
    }

    @Test
    void storeWritesValidJsonAndEscapesQuotes() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        Message message = new Message("0012345678", "+27718693002",
                "Hi Mike, \"see you\" tonight?", 0);

        assertEquals("Message successfully stored.",
                message.SentMessage("STORE", jsonFile));

        String json = Files.readString(jsonFile);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]\n"));
        assertTrue(json.contains("\"messageId\":\"0012345678\""));
        assertTrue(json.contains("\\\"see you\\\""));
        assertTrue(json.contains("\"status\":\"STORED\""));
        assertEquals(0, message.returnTotalMessages());
        Message restored =
                new MessageStore(jsonFile).readStoredMessages().get(0);
        assertEquals("Hi Mike, \"see you\" tonight?",
                restored.getMessageText());
        assertEquals("Unknown", restored.getSender());
    }

    @Test
    void generatedIdsAreTenDigits() {
        Message message = new Message("+27718693002", "Hello", 0);
        assertTrue(message.checkMessageID());
    }

    @Test
    void selectedMessageCountRunsThenMenuWaitsForQuit() {
        String input = String.join("\n",
                "2",
                "1",
                "+27718693002",
                "Hi Mike, can you join us for dinner tonight?",
                "1",
                "08575975889",
                "+27857575889",
                "Hi Keegan, did you receive the payment?",
                "2",
                "0",
                "2",
                "4",
                "7",
                "3",
                "");
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (Scanner scanner = new Scanner(new ByteArrayInputStream(
                     input.getBytes(StandardCharsets.UTF_8)));
             PrintStream testOutput = new PrintStream(capturedOutput, true,
                     StandardCharsets.UTF_8)) {
            System.setOut(testOutput);
            new ChatApp(scanner, "Jeffrey").run();
        } finally {
            System.setOut(originalOutput);
        }

        String output = capturedOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Message successfully sent."));
        assertTrue(output.contains("Please correct the number and try again."));
        assertTrue(output.contains("Message deleted."));
        assertTrue(output.contains("Total messages sent: 1"));
        assertTrue(output.contains("Hi Mike, can you join us for dinner tonight?"));
        assertTrue(output.contains("=== Stored Messages ==="));
        assertTrue(output.contains("Goodbye, Jeffrey."));
        assertEquals(2, output.split("Message ID generated: ", -1).length - 1);
    }

    @Test
    void arraysPopulateFromSampleActionsAndStoredJson() throws Exception {
        MessageArrays arrays = new MessageArrays();
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        MessageStore store = new MessageStore(jsonFile);

        Message message1 = new Message("0000000001", "Developer",
                "+27834557896", "Did you get the cake?", 0);
        arrays.recordCreatedMessage(message1);
        assertEquals("Message successfully sent.",
                message1.SentMessage("SEND", jsonFile));
        arrays.recordSentMessage(message1);

        Message message2 = new Message("0000000002", "Developer",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                1);
        arrays.recordCreatedMessage(message2);
        assertEquals("Message successfully stored.",
                message2.SentMessage("STORE", jsonFile));

        Message message3 = new Message("0000000003", "Developer",
                "+27834484567", "Yohoooo, I am at your gate.", 2);
        arrays.recordCreatedMessage(message3);
        assertTrue(message3.markAsDisregarded());
        arrays.recordDisregardedMessage(message3);

        Message message4 = Message.restore("0838884567", "Developer",
                "0838884567", "It is dinner time !", 3, "SENT", null,
                Instant.EPOCH);
        arrays.recordCreatedMessage(message4);
        arrays.recordSentMessage(message4);

        Message message5 = new Message("0000000005", "Developer",
                "+27838884567", "Ok, I am leaving without you.", 4);
        arrays.recordCreatedMessage(message5);
        assertEquals("Message successfully stored.",
                message5.SentMessage("STORE", jsonFile));
        arrays.refreshStoredMessages(store);

        assertEquals(2, arrays.getSentMessagesArray().length);
        assertEquals(1, arrays.getDisregardedMessagesArray().length);
        assertEquals(2, arrays.getStoredMessagesArray().length);
        assertEquals(5, arrays.getMessageIdsArray().length);
        assertEquals(5, arrays.getMessageHashesArray().length);
        assertEquals("Developer",
                arrays.getStoredMessagesArray()[0].getSender());
        assertEquals("+27838884567",
                arrays.getStoredMessagesArray()[0].getRecipient());
    }

    @Test
    void longestStoredMessageIsFoundAfterReadingJson() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        MessageStore store = new MessageStore(jsonFile);
        Message shorter = new Message("0000000005", "Developer",
                "+27838884567", "Ok, I am leaving without you.", 4);
        Message longer = new Message("0000000002", "Developer",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                1);
        shorter.storeMessage(jsonFile);
        longer.storeMessage(jsonFile);

        MessageArrays arrays = new MessageArrays();
        arrays.refreshStoredMessages(store);

        assertEquals(longer.getMessageId(),
                arrays.findLongestStoredMessage().orElseThrow()
                        .getMessageId());
    }

    @Test
    void messageIdSearchFindsTheSentSampleMessage() throws Exception {
        MessageArrays arrays = new MessageArrays();
        Message sentSample = Message.restore("0838884567", "Developer",
                "0838884567", "It is dinner time !", 3, "SENT", null,
                Instant.EPOCH);
        arrays.recordCreatedMessage(sentSample);
        arrays.recordSentMessage(sentSample);

        Message found = arrays.findMessageById("0838884567").orElseThrow();
        assertEquals("0838884567", found.getRecipient());
        assertEquals("It is dinner time !", found.getMessageText());
    }

    @Test
    void recipientSearchReturnsMatchingSentAndStoredMessages() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        MessageStore store = new MessageStore(jsonFile);
        MessageArrays arrays = new MessageArrays();
        Message stored1 = new Message("0000000002", "Developer",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                1);
        Message stored2 = new Message("0000000005", "Developer",
                "+27838884567", "Ok, I am leaving without you.", 4);
        stored1.storeMessage(jsonFile);
        stored2.storeMessage(jsonFile);
        Message sent = Message.restore("0000000006", "Developer",
                "+27838884567", "Another sent message.", 5, "SENT", null,
                Instant.EPOCH);
        arrays.recordSentMessage(sent);
        arrays.refreshStoredMessages(store);

        List<Message> matches =
                arrays.findSentOrStoredMessagesByRecipient("+27838884567");

        assertEquals(3, matches.size());
        assertTrue(matches.stream().anyMatch(message ->
                message.getMessageText().startsWith("Where are you?")));
        assertTrue(matches.stream().anyMatch(message ->
                message.getMessageText().equals("Ok, I am leaving without you.")));
        assertTrue(matches.stream().anyMatch(message ->
                message.getMessageText().equals("Another sent message.")));
    }

    @Test
    void storedMessageCanBeDeletedUsingItsHash() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        MessageStore store = new MessageStore(jsonFile);
        Message toDelete = new Message("0000000002", "Developer",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                1);
        Message toKeep = new Message("0000000005", "Developer",
                "+27838884567", "Ok, I am leaving without you.", 4);
        toDelete.storeMessage(jsonFile);
        toKeep.storeMessage(jsonFile);

        Message deleted = store.deleteStoredMessageByHash(
                toDelete.createMessageHash()).orElseThrow();

        assertEquals(toDelete.getMessageText(), deleted.getMessageText());
        assertEquals(1, store.readStoredMessages().size());
        assertEquals(toKeep.getMessageId(),
                store.readStoredMessages().get(0).getMessageId());
    }

    @Test
    void reportIncludesFullDetailsForSentAndStoredMessages() throws Exception {
        MessageArrays arrays = new MessageArrays();
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        MessageStore store = new MessageStore(jsonFile);
        Message sent = new Message("0000000001", "Developer",
                "+27834557896", "Did you get the cake?", 0);
        arrays.recordSentMessage(sent);
        Message stored = new Message("0000000002", "Developer",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                1);
        stored.storeMessage(jsonFile);
        arrays.refreshStoredMessages(store);

        String report = arrays.createMessageReport();

        assertTrue(report.contains("Message ID: 0000000001"));
        assertTrue(report.contains("Message Hash: "
                + sent.createMessageHash()));
        assertTrue(report.contains("Sender: Developer"));
        assertTrue(report.contains("Recipient: +27834557896"));
        assertTrue(report.contains("Did you get the cake?"));
        assertTrue(report.contains("Where are you? You are late!"));
    }

    @Test
    void storedMessagesMenuRunsAllRequiredActions() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        MessageStore store = new MessageStore(jsonFile);
        Message longer = new Message("0000000002", "Developer",
                "+27838884567",
                "Where are you? You are late! I have asked you to be on time.",
                1);
        Message shorter = new Message("0000000005", "Developer",
                "+27838884567", "Ok, I am leaving without you.", 4);
        longer.storeMessage(jsonFile);
        shorter.storeMessage(jsonFile);

        String input = String.join("\n",
                "0",
                "4",
                "1",
                "2",
                "3",
                "0000000002",
                "4",
                "+27838884567",
                "6",
                "5",
                longer.createMessageHash(),
                "7",
                "3",
                "");
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (Scanner scanner = new Scanner(new ByteArrayInputStream(
                     input.getBytes(StandardCharsets.UTF_8)));
             PrintStream testOutput = new PrintStream(capturedOutput, true,
                     StandardCharsets.UTF_8)) {
            System.setOut(testOutput);
            new ChatApp(scanner, "Developer", jsonFile).run();
        } finally {
            System.setOut(originalOutput);
        }

        String output = capturedOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(
                "Sender: Developer | Recipient: +27838884567"));
        assertTrue(output.contains("Where are you? You are late!"));
        assertTrue(output.contains("Message ID: 0000000002"));
        assertTrue(output.contains("Ok, I am leaving without you."));
        assertTrue(output.contains("successfully deleted."));
        assertTrue(output.contains("Message Hash: " + shorter.createMessageHash()));
        assertEquals(1, store.readStoredMessages().size());
        assertEquals("0000000005",
                store.readStoredMessages().get(0).getMessageId());
    }
}
