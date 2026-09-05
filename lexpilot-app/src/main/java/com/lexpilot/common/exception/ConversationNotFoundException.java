package com.lexpilot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a conversation session ID is invalid or does not exist.
 * Maps to HTTP 404 Not Found with error code LP-4042.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConversationNotFoundException extends LexPilotException {

    public ConversationNotFoundException(String sessionId) {
        super("Conversation not found: " + sessionId, "LP-4042");
    }
}
