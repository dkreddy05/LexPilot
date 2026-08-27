package com.lexpilot.generation.prompt;

/**
 * A single message in a chat-completion prompt.
 *
 * @param role    SYSTEM or USER
 * @param content the message text
 */
public record PromptMessage(Role role, String content) {

    public enum Role { SYSTEM, USER, ASSISTANT }
}
