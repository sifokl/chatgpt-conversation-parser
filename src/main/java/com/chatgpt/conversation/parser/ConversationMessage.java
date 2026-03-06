package com.chatgpt.conversation.parser;

public final class ConversationMessage {

    public enum Role {
        USER,
        ASSISTANT,
        UNKNOWN
    }

    private final Role role;
    private final String text;

    public ConversationMessage(Role role, String text) {
        this.role = role;
        this.text = text;
    }

    public Role getRole() {
        return role;
    }

    public String getText() {
        return text;
    }
}
