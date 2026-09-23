package com.dashenbank.cms.exception;

import org.springframework.http.HttpStatus;

public class CbsException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public CbsException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static CbsException unavailable() {
        return new CbsException("CBS_UNAVAILABLE",
                "Core Banking is currently unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static CbsException timeout() {
        return new CbsException("CBS_TIMEOUT",
                "Core Banking lookup timed out. Please try again later.",
                HttpStatus.GATEWAY_TIMEOUT);
    }
}
