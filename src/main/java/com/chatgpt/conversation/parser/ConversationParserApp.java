package com.chatgpt.conversation.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConversationParserApp {

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            printUsage();
            System.exit(1);
        }

        Path rootPath = Paths.get(args[0]);
        if (!Files.isDirectory(rootPath)) {
            System.err.printf("Provided path is not a directory: %s%n", rootPath);
            printUsage();
            System.exit(1);
        }

        Path baseOutputPath = resolveBaseOutputPath(rootPath, args);
        ConversationScanner scanner = new ConversationScanner();

        try {
            Path runOutputPath = prepareRunOutputPath(baseOutputPath);
            List<Conversation> conversations = scanner.scanRoot(rootPath);

            if (conversations.isEmpty()) {
                System.out.println("No ChatGPT conversations were detected under the provided root.");
                return;
            }

            for (Conversation conversation : conversations) {
                List<String> conversationLines = formatConversation(conversation);
                Path conversationFile = runOutputPath.resolve(toOutputFileName(conversation.getSource().getHtmlFileName()));
                Files.write(conversationFile, conversationLines, StandardCharsets.UTF_8);
                System.out.println("Wrote: " + conversationFile);
            }

            System.out.println("Execution output folder: " + runOutputPath);
        } catch (IOException e) {
            System.err.println("Failed to scan conversations: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static Path prepareRunOutputPath(Path baseOutputPath) throws IOException {
        Files.createDirectories(baseOutputPath);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        Path runOutputPath = baseOutputPath.resolve(timestamp);
        Files.createDirectories(runOutputPath);
        return runOutputPath;
    }

    private static Path resolveBaseOutputPath(Path rootPath, String[] args) {
        if (args.length == 2) {
            return Paths.get(args[1]);
        }
        return rootPath.resolve("chatgpt-conversations-parser-OUTPUT");
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar chatgpt-conversation-parser.jar <root-directory>");
        System.out.println("The tool walks every subfolder under <root-directory> looking for saved ChatGPT HTML exports.");
        System.out.println("Optionally provide a second argument for the output directory.");
    }

    private static String toOutputFileName(String htmlFileName) {
        return htmlFileName.replaceFirst("(?i)\\.html?$", "") + ".txt";
    }

    private static List<String> formatConversation(Conversation conversation) {
        List<String> lines = new ArrayList<>();
        lines.add("=== " + conversation.getTitle() + " ===");
        lines.add("");
        boolean attachmentsPrinted = false;

        if (conversation.getMessages().isEmpty()) {
            lines.add("No messages were extracted from the HTML export.");
            return lines;
        }

        for (ConversationMessage message : conversation.getMessages()) {
            appendMessage(lines, message);

            if (!attachmentsPrinted
                    && message.getRole() == ConversationMessage.Role.USER
                    && !conversation.getAttachments().isEmpty()) {
                lines.add(String.format("pieces jointes : %s", String.join(", ", conversation.getAttachments())));
                lines.add("");
                attachmentsPrinted = true;
            }
        }
        return lines;
    }

    private static void appendMessage(List<String> lines, ConversationMessage message) {
        String prefix = message.getRole() == ConversationMessage.Role.USER ? "user" : "chatgpt";
        lines.add(String.format("%s said :", prefix));
        lines.add(message.getText());
        lines.add("");
    }
}
