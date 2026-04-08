package com.insuraTrack.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Duplicate key / constraint violations ──────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex) {

        String cause = ex.getMostSpecificCause().getMessage();
        String message = "A duplicate entry was detected. Please check your input.";

        // Map known constraint names to friendly messages
        if (cause != null) {
            if (cause.contains("policy_number"))  message = "A policy with this policy number already exists.";
            else if (cause.contains("uk_"))       message = "This entry already exists (unique constraint violated).";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── Business-logic errors thrown manually (e.g. throw new RuntimeException("...")) ──
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ── Catch-all ──────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "An unexpected error occurred. Please try again.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}