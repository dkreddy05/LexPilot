package com.lexpilot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class UpstreamServiceException extends LexPilotException {

    public UpstreamServiceException(String serviceName, Throwable cause) {
        super("Upstream service unavailable: " + serviceName, "LP-5031", cause);
    }
}
