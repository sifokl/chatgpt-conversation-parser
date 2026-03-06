package com.chatgpt.conversation.parser;

import java.util.Collections;
import java.util.List;

public final class Conversation {
    private final String title;
    private final PathReference source;
    private final List<ConversationMessage> messages;
    private final List<String> attachments;

    public Conversation(String title,
                        PathReference source,
                        List<ConversationMessage> messages,
                        List<String> attachments) {
        this.title = title;
        this.source = source;
        this.messages = Collections.unmodifiableList(messages);
        this.attachments = Collections.unmodifiableList(attachments);
    }

    public String getTitle() {
        return title;
    }

    public PathReference getSource() {
        return source;
    }

    public List<ConversationMessage> getMessages() {
        return messages;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    /**
     * Lightweight holder so callers can know where the HTML came from without exposing Path directly.
     */
    public static final class PathReference {
        private final String directory;
        private final String htmlFileName;

        public PathReference(String directory, String htmlFileName) {
            this.directory = directory;
            this.htmlFileName = htmlFileName;
        }

        public String getDirectory() {
            return directory;
        }

        public String getHtmlFileName() {
            return htmlFileName;
        }
    }
}
