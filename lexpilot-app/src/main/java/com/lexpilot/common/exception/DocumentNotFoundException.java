package com.lexpilot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentNotFoundException extends LexPilotException {

    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId, "LP-4041");
    }
}
