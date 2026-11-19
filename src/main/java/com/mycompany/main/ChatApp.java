package com.mycompany.main;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console entry point: Part 1 registration/login followed by Part 2 messaging.
 */
public class ChatApp {
    private static final Path MESSAGE_FILE = Path.of("messages.json");

    private final Scanner scanner;
    private final String firstName;
    private final Path messageFile;
    private final MessageArrays messageArrays;
    private final MessageStore messageStore;
    private String storedMessagesError;

    public ChatApp(Scanner scanner, String firstName) {
        this(scanner, firstName, MESSAGE_FILE);
    }

    ChatApp(Scanner scanner, String firstName, Path messageFile) {
        this.scanner = scanner;
        this.firstName = firstName;
        this.messageFile = messageFile;
        this.messageArrays = new MessageArrays();
        this.messageStore = new MessageStore(messageFile);
        refreshStoredMessages();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Login user = registerAndLogin(scanner);
        if (user != null) {
            new ChatApp(scanner, user.getFirstName()).run();
        }
        scanner.close();
    }

    private static Login registerAndLogin(Scanner scanner) {
        Login user;
        while (true) {
            System.out.println("=== Registration ===");
            System.out.print("Enter first name: ");
            String firstName = scanner.nextLine().trim();
            System.out.print("Enter last name: ");
            String lastName = scanner.nextLine().trim();
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            System.out.print("Enter cell phone number (+27...): ");
            String cellPhoneNumber = scanner.nextLine().trim();

            user = new Login(username, password, cellPhoneNumber,
                    firstName, lastName);
            String registrationMessage = user.registerUser();
            System.out.println(registrationMessage);
            if ("User registered successfully.".equals(registrationMessage)) {
                break;
            }
            System.out.println("Please enter your registration details again.\n");
        }
        
        boolean loginStatus = false;
        while (!loginStatus) {
            System.out.println("\n=== Login ===");
            System.out.print("Enter username: ");
            String loginUsername = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String loginPassword = scanner.nextLine();

            loginStatus = user.loginUser(loginUsername, loginPassword);
            System.out.println(user.returnLoginStatus(loginStatus));
        }
        return user;
    }

    public void run() {
        System.out.println("\nWelcome to the ChatApp, " + firstName + "!");
        int messageLimit = readNonNegativeInt(
                "How many messages would you like to enter? ");
        int messagesCreated = 0;
        boolean running = true;

        // The menu continues until the user chooses option 3.
        while (running) {
            printMenu();
            int option = readInt("Choose an option (1-4): ");
            switch (option) {
                case 1:
                    if (messagesCreated >= messageLimit) {
                        System.out.println("You have reached your message limit "
                                + "of " + messageLimit + ".");
                    } else {
                        int totalMessagesSent = 0;

                        // Process only the number of messages selected at startup.
                        for (; messagesCreated < messageLimit; messagesCreated++) {
                            totalMessagesSent =
                                    createAndProcessMessage(messagesCreated);
                        }

                        System.out.println("Total messages sent: "
                                + totalMessagesSent);
                    }
                    break;
                case 2:
                    displaySentMessages();
                    break;
                case 3:
                    System.out.println("Goodbye, " + firstName + ".");
                    running = false;
                    break;
                case 4:
                    runStoredMessagesMenu();
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, 3, or 4.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== ChatApp Menu ===");
        System.out.println("1) Send Messages");
        System.out.println("2) Show recently sent messages");
        System.out.println("3) Quit");
        System.out.println("4) Stored Messages");
    }

    private int createAndProcessMessage(int messageNumber) {
        System.out.println("\n--- Message " + (messageNumber + 1) + " ---");
        String recipient = readRecipientNumber();
        String messageText = readMessageText();
        Message message = new Message(recipient, messageText, messageNumber,
                firstName);
        messageArrays.recordCreatedMessage(message);

        System.out.println("Message ID generated: " + message.getMessageId());
        System.out.println("Message Hash: " + message.createMessageHash());
        System.out.println("1) Send Message");
        System.out.println("2) Disregard Message");
        System.out.println("3) Store Message to JSON");
        int action = readInt("Choose an action (1-3): ");

        String result;
        switch (action) {
            case 1:
                result = message.SentMessage("SEND", messageFile);
                System.out.println(result);
                if ("Message successfully sent.".equals(result)) {
                    messageArrays.recordSentMessage(message);
                    System.out.println(message.formatDetails());
                }
                break;
            case 2:
                result = message.SentMessage("DISCARD", messageFile);
                System.out.println(result);
                if ("Press 0 to delete the message.".equals(result)) {
                    String confirmation = readLine(
                            "Enter 0 to delete this message: ").trim();
                    if ("0".equals(confirmation)
                            && message.markAsDisregarded()) {
                        messageArrays.recordDisregardedMessage(message);
                        System.out.println("Message deleted.");
                    } else {
                        System.out.println("Message was not deleted.");
                    }
                }
                break;
            case 3:
                result = message.SentMessage("STORE", messageFile);
                System.out.println(result);
                if ("Message successfully stored.".equals(result)) {
                    refreshStoredMessages();
                }
                break;
            default:
                System.out.println("Invalid action. Message was not sent.");
        }

        return message.returnTotalMessages();
    }

    private void displaySentMessages() {
        Message[] sentMessages = messageArrays.getSentMessagesArray();
        if (sentMessages.length == 0) {
            System.out.println("No messages have been sent yet.");
            return;
        }
        for (Message message : sentMessages) {
            System.out.println(message.formatDetails());
            System.out.println();
        }
    }

    private void runStoredMessagesMenu() {
        refreshStoredMessages();
        if (storedMessagesError != null) {
            System.out.println("Could not read stored messages: "
                    + storedMessagesError);
        }

        boolean inStoredMenu = true;
        while (inStoredMenu) {
            printStoredMessagesMenu();
            int option = readInt("Choose an option (1-7): ");
            switch (option) {
                case 1:
                    displayStoredSendersAndRecipients();
                    break;
                case 2:
                    displayLongestStoredMessage();
                    break;
                case 3:
                    searchStoredMessageById();
                    break;
                case 4:
                    searchMessagesByRecipient();
                    break;
                case 5:
                    deleteStoredMessageByHash();
                    break;
                case 6:
                    displayMessageReport();
                    break;
                case 7:
                    inStoredMenu = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1 to 7.");
            }
        }
    }

    private void printStoredMessagesMenu() {
        System.out.println("\n=== Stored Messages ===");
        System.out.println("1) Display sender and recipient of stored messages");
        System.out.println("2) Display the longest stored message");
        System.out.println("3) Search messages by message ID");
        System.out.println("4) Search sent and stored messages by recipient");
        System.out.println("5) Delete a stored message using its hash");
        System.out.println("6) Display the message report");
        System.out.println("7) Return to the main menu");
    }

    private void displayStoredSendersAndRecipients() {
        Message[] storedMessages = messageArrays.getStoredMessagesArray();
        if (storedMessages.length == 0) {
            System.out.println("No stored messages were found.");
            return;
        }
        for (Message message : storedMessages) {
            System.out.println("Sender: " + message.getSender()
                    + " | Recipient: " + message.getRecipient());
        }
    }

    private void displayLongestStoredMessage() {
        Optional<Message> longest =
                messageArrays.findLongestStoredMessage();
        if (longest.isEmpty()) {
            System.out.println("No stored messages were found.");
            return;
        }
        System.out.println(longest.get().formatFullDetails());
    }

    private void searchStoredMessageById() {
        String messageId = readLine("Enter the message ID: ").trim();
        Optional<Message> result =
                messageArrays.findMessageById(messageId);
        if (result.isEmpty()) {
            System.out.println("No message was found with that ID.");
            return;
        }
        Message message = result.get();
        System.out.println("Message ID: " + message.getMessageId()
                + "\nRecipient: " + message.getRecipient()
                + "\nMessage: " + message.getMessageText());
    }

    private void searchMessagesByRecipient() {
        String recipient = readLine("Enter the recipient number: ").trim();
        List<Message> matches =
                messageArrays.findSentOrStoredMessagesByRecipient(recipient);
        if (matches.isEmpty()) {
            System.out.println("No sent or stored messages were found for "
                    + recipient + ".");
            return;
        }
        for (Message message : matches) {
            System.out.println(message.formatFullDetails());
            System.out.println();
        }
    }

    private void deleteStoredMessageByHash() {
        String messageHash = readLine("Enter the message hash to delete: ")
                .trim();
        try {
            Optional<Message> deleted =
                    messageStore.deleteStoredMessageByHash(messageHash);
            if (deleted.isEmpty()) {
                System.out.println("No stored message was found with that hash.");
                return;
            }
            refreshStoredMessages();
            System.out.println("Message: \"" + deleted.get().getMessageText()
                    + "\" successfully deleted.");
        } catch (IOException exception) {
            System.out.println("The stored message could not be deleted: "
                    + exception.getMessage());
        }
    }

    private void displayMessageReport() {
        System.out.println(messageArrays.createMessageReport());
    }

    private void refreshStoredMessages() {
        try {
            messageArrays.refreshStoredMessages(messageStore);
            storedMessagesError = null;
        } catch (IOException exception) {
            storedMessagesError = exception.getMessage();
        }
    }

    private String readRecipientNumber() {
    while (true) {
        String recipient = readLine(
                "Enter recipient number (+27 followed by 9 digits): ")
                .trim();

        if (Message.isRecipientCellValid(recipient)) {
            return recipient;
        }

        System.out.println("Cell phone number is incorrectly formatted or "
                + "does not contain an international code. Please correct "
                + "the number and try again.");
    }
}
    
    private String readMessageText() {
        while (true) {
            String messageText = readLine("Enter your message (maximum 250 "
                    + "characters): ");
            if (messageText.length() <= 250) {
                return messageText;
            }
            System.out.println("Please enter a message of less than 250 "
                    + "characters.");
        }
    }

    private int readNonNegativeInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println("Please enter zero or a positive whole number.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt).trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a numeric option.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}
