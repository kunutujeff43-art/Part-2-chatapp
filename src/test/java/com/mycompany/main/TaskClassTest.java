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
        assertTrue(output.contains("Coming Soon."));
        assertTrue(output.contains("Goodbye, Jeffrey."));
        assertEquals(2, output.split("Message ID generated: ", -1).length - 1);
    }
}
