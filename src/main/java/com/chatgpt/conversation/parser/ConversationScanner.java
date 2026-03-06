package com.chatgpt.conversation.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConversationScanner {
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg"));

    /**
     * Walks every first-level subdirectory under {@code rootPath} and tries to parse the first HTML export it finds.
     */
    public List<Conversation> scanRoot(Path rootPath) throws IOException {
        List<Conversation> conversations = new ArrayList<>();

        List<Path> htmlFilesInRoot = locateHtmlFiles(rootPath);
        conversations.addAll(parseHtmlFiles(rootPath, htmlFilesInRoot));

        try (var entries = Files.list(rootPath)) {
            for (Path entry : entries.collect(Collectors.toList())) {
                if (!Files.isDirectory(entry)) {
                    continue;
                }

                List<Path> htmlFiles = locateHtmlFiles(entry);
                conversations.addAll(parseHtmlFiles(entry, htmlFiles));
            }
        }

        return conversations;
    }

    private List<Path> locateHtmlFiles(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(this::isHtml)
                    .collect(Collectors.toList());
        }
    }

    private boolean isHtml(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".html") || name.endsWith(".htm");
    }

    private List<Conversation> parseHtmlFiles(Path directory, List<Path> htmlFiles) throws IOException {
        List<Conversation> conversations = new ArrayList<>();
        for (Path htmlFile : htmlFiles) {
            conversations.add(parseConversation(directory, htmlFile));
        }
        return conversations;
    }

    private Conversation parseConversation(Path directory, Path htmlFile) throws IOException {
        Document document = Jsoup.parse(htmlFile.toFile(), "UTF-8");
        String title = extractTitle(document, htmlFile);
        List<ConversationMessage> messages = extractMessages(document);
        List<String> attachments = collectAttachments(directory);

        Conversation.PathReference pathReference = new Conversation.PathReference(directory.toString(), htmlFile.getFileName().toString());
        return new Conversation(title, pathReference, messages, attachments);
    }

    private String extractTitle(Document document, Path htmlFile) {
        String title = document.title();
        if (title != null && !title.isBlank()) {
            return title.strip();
        }

        Element heading = document.selectFirst("h1");
        if (heading != null && !heading.text().isBlank()) {
            return heading.text().strip();
        }

        return htmlFile.getFileName().toString();
    }

    private List<ConversationMessage> extractMessages(Document document) {
        List<ConversationMessage> messages = new ArrayList<>();

        Elements entries = document.select("div[data-author], div[data-role=message]");
        if (entries.isEmpty()) {
            entries = document.select("div[class*=message], article");
        }

        for (Element entry : entries) {
            String text = entry.text().trim();
            if (text.isEmpty()) {
                continue;
            }

            ConversationMessage.Role role = detectRole(entry);
            messages.add(new ConversationMessage(role, text));
        }

        // TODO: Enhance selectors to match the latest ChatGPT DOM and to differentiate inline metadata better.
        return messages;
    }

    private ConversationMessage.Role detectRole(Element entry) {
        String author = entry.attr("data-author");
        if (author.isBlank()) {
            author = extractAuthorFromClasses(entry);
        }

        return switch (author.toLowerCase()) {
            case "user" -> ConversationMessage.Role.USER;
            case "assistant", "ai", "chatgpt" -> ConversationMessage.Role.ASSISTANT;
            default -> ConversationMessage.Role.UNKNOWN;
        };
    }

    private String extractAuthorFromClasses(Element entry) {
        for (String className : entry.classNames()) {
            String normalized = className.toLowerCase();
            if (normalized.contains("user")) {
                return "user";
            }
            if (normalized.contains("assistant") || normalized.contains("ai") || normalized.contains("bot")) {
                return "assistant";
            }
        }
        return "unknown";
    }

    private List<String> collectAttachments(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(this::isImage)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        }
    }

    // TODO: Analyze inline metadata to tie attachments to the exact user message that submitted them.
    private boolean isImage(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot == -1 || dot == name.length() - 1) {
            return false;
        }
        String extension = name.substring(dot + 1).toLowerCase();
        return IMAGE_EXTENSIONS.contains(extension);
    }
}
