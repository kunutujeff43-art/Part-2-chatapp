# ChatApp — Part 3

This Maven project continues the supplied Part 2 ChatApp in the original
`com.mycompany.main` package. It retains the Part 1 `+27` recipient validation,
message ID/hash generation, and the existing send/store/disregard flow.

## Run in NetBeans

1. Open this folder as a Maven project.
2. Run `com.mycompany.main.ChatApp`.
3. The main menu keeps **Quit** at option 3 and adds **Stored Messages** as
   option 4. From there, use the submenu to
   display stored senders and recipients, find the longest stored message,
   search by ID or recipient, delete by hash, or print the report.
4. Stored messages are written to and read from `messages.json` in the
   program's working directory.

The project targets Java 17 or newer. From a terminal, run:

```sh
mvn clean test
```

The GitHub Actions workflow at `.github/workflows/TestJava.yml` runs the same
test command on pushes, pull requests, and manual runs.

## Part 3 data

`MessageArrays` uses dynamically populated Java arrays for sent messages,
disregarded messages, stored messages, message hashes, and message IDs. The
stored-message array is populated by reading the JSON file; it is not seeded
with hard-coded application data.

The five assignment examples are represented in `TaskClassTest.java`. Message
4 uses the supplied value `0838884567` for its message ID and test recipient,
as requested. The original live-send validation remains unchanged and accepts
only `+27` followed by nine digits, so this local-format sample is used for
lookup/report tests rather than passed through the send validator.

## Adding Part 3 to an existing Part 1 project

If you are copying files into your existing NetBeans project, keep your own
Part 1 `Login.java`. Do not add a second `Login` class to the same package.
Replace `ChatApp.java`, `Message.java`, and `MessageStore.java`; add
`MessageArrays.java`; and replace the Part 2 `TaskClassTest.java` with the
updated test class. Keep your Maven project configuration and add the Java
compiler and Surefire plugins shown in this project's `pom.xml` if they are
missing. Put `TestJava.yml` in `.github/workflows/`.
