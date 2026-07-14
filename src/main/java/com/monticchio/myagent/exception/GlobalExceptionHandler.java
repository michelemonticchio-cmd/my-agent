package com.monticchio.myagent.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(String error) {}

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ErrorResponse> handleLlmException(LlmException e) {
        log.error("Call to Anthropic API failed", e);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("AI service unavailable, please try again later"));
    }
}
