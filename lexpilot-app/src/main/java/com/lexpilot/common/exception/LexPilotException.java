package com.lexpilot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class LexPilotException extends RuntimeException {

    private final String errorCode;

    public LexPilotException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public LexPilotException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
