package com.monticchio.myagent.exception;

public class LlmException extends RuntimeException {
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
