package com.dashenbank.cms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    @ExceptionHandler(FeedbackTokenException.class)
    public ResponseEntity<Map<String, Object>> handleFeedbackTokenException(FeedbackTokenException ex,
            HttpServletRequest request) {
        log.warn("Feedback Token Warning [{} {}]: Code={} Message={}",
                request.getMethod(), request.getRequestURI(), ex.getCode(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_SUCCESS, false);
        body.put("code", ex.getCode());
        body.put(KEY_MESSAGE, ex.getMessage());
        body.put(KEY_TIMESTAMP, currentTimestamp());

        return new ResponseEntity<>(body, ex.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Invalid Argument Warning [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_SUCCESS, false);
        body.put("code", "INVALID_REQUEST_PAYLOAD");
        body.put(KEY_MESSAGE, ex.getMessage());
        body.put(KEY_TIMESTAMP, currentTimestamp());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        log.warn("Validation failed [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_SUCCESS, false);
        body.put("code", "VALIDATION_ERROR");
        body.put(KEY_MESSAGE, message);
        body.put("error", message);
        body.put(KEY_TIMESTAMP, currentTimestamp());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex,
            HttpServletRequest request) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        log.warn("Request rejected [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_SUCCESS, false);
        body.put("code", "REQUEST_REJECTED");
        body.put(KEY_MESSAGE, message);
        body.put("error", message);
        body.put(KEY_TIMESTAMP, currentTimestamp());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[TRACE-{}] Internal Server Exception [{} {}]: ",
                traceId, request.getMethod(), request.getRequestURI(), ex);

        String detailMsg = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage()
                : "An unexpected server error occurred. Please try again later.";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_SUCCESS, false);
        body.put("code", "INTERNAL_SERVER_ERROR");
        body.put(KEY_MESSAGE, detailMsg);
        body.put("error", detailMsg);
        body.put("traceId", traceId);
        body.put(KEY_TIMESTAMP, currentTimestamp());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static String currentTimestamp() {
        return LocalDateTime.now(SYSTEM_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
