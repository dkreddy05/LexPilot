package com.lexpilot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when Tika text extraction fails or produces empty/garbage output
 * (e.g. scanned PDF with no embedded text layer).
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExtractionException extends LexPilotException {

    public ExtractionException(String message) {
        super(message, "LP-5010");
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, "LP-5010", cause);
    }
}
