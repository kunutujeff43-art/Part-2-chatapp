package com.mycompany.main;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Console entry point: Part 1 registration/login followed by Part 2 messaging.
 */
public class ChatApp {
    private static final Path MESSAGE_FILE = Path.of("messages.json");

    private final Scanner scanner;
    private final String firstName;

    public ChatApp(Scanner scanner, String firstName) {
        this.scanner = scanner;
        this.firstName = firstName;
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
        return loginStatus ? user : null;
       
    }
        return null;
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
            int option = readInt("Choose an option (1-3): ");
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
                    System.out.println("Coming Soon.");
                    break;
                case 3:
                    System.out.println("Goodbye, " + firstName + ".");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, or 3.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== ChatApp Menu ===");
        System.out.println("1) Send Messages");
        System.out.println("2) Show recently sent messages");
        System.out.println("3) Quit");
    }

    private int createAndProcessMessage(int messageNumber) {
        System.out.println("\n--- Message " + (messageNumber + 1) + " ---");
       String recipient = readRecipientNumber();
        String messageText = readMessageText();
        Message message = new Message(recipient, messageText, messageNumber);

        System.out.println("Message ID generated: " + message.getMessageId());
        System.out.println("Message Hash: " + message.createMessageHash());
        System.out.println("1) Send Message");
        System.out.println("2) Disregard Message");
        System.out.println("3) Store Message to JSON");
        int action = readInt("Choose an action (1-3): ");

        String result;
        switch (action) {
            case 1:
                result = message.SentMessage("SEND", MESSAGE_FILE);
                System.out.println(result);
                if ("Message successfully sent.".equals(result)) {
                    System.out.println(message.formatDetails());
                }
                break;
            case 2:
                result = message.SentMessage("DISCARD", MESSAGE_FILE);
                System.out.println(result);
                if ("Press 0 to delete the message.".equals(result)) {
                    String confirmation = readLine(
                            "Enter 0 to delete this message: ").trim();
                    System.out.println("0".equals(confirmation)
                            ? "Message deleted."
                            : "Message was not deleted.");
                }
                break;
            case 3:
                result = message.SentMessage("STORE", MESSAGE_FILE);
                System.out.println(result);
                break;
            default:
                System.out.println("Invalid action. Message was not sent.");
        }

        return message.returnTotalMessages();
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
