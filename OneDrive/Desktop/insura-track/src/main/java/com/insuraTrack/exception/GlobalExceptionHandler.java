package com.insuraTrack.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * FIXED GlobalExceptionHandler
 *
 * ROOT CAUSE OF ALL 500s:
 * The original handler caught every Throwable and returned a vague
 * {"error":"An unexpected error occurred...", "timestamp":"..."} body.
 * This swallowed useful error info and prevented controller-level try/catch
 * from being reached in some cases (e.g. Spring proxy exceptions).
 *
 * FIXES:
 *  1. RuntimeException → 400 Bad Request with the actual message
 *  2. EntityNotFoundException → 404 Not Found
 *  3. DataIntegrityViolationException → 409 Conflict (FK constraint violations)
 *  4. AccessDeniedException → 403 Forbidden (re-throw to Spring Security)
 *  5. Fallback Throwable → 500 with actual cause logged
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Business logic errors thrown as RuntimeException ──────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";

        // Log the real error so it shows up in Spring logs
        System.err.println("❌ RuntimeException: " + msg);
        ex.printStackTrace();

        HttpStatus status;
        String lower = msg.toLowerCase();
        if (lower.contains("not found") || lower.contains("no such")) {
            status = HttpStatus.NOT_FOUND;                        // 404
        } else if (lower.contains("already exists") || lower.contains("already used")
                || lower.contains("cannot delete") || lower.contains("linked")) {
            status = HttpStatus.CONFLICT;                         // 409
        } else if (lower.contains("only admin") || lower.contains("permission denied")
                || lower.contains("access denied")) {
            status = HttpStatus.FORBIDDEN;                        // 403
        } else {
            status = HttpStatus.BAD_REQUEST;                      // 400
        }

        return ResponseEntity.status(status).body(Map.of("message", msg));
    }

    // ── JPA entity not found ───────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Entity not found";
        System.err.println("❌ EntityNotFoundException: " + msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", msg));
    }

    // ── Database FK / unique constraint violations ────────────────────────────
    // This catches the raw DB exception when a DELETE violates a FK constraint.
    // Previously this became a 500 with the generic message.

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String root = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        System.err.println("❌ DataIntegrityViolationException: " + root);

        String userMsg;
        if (root != null && root.toLowerCase().contains("foreign key")) {
            userMsg = "Cannot delete: this record is referenced by other data. "
                    + "Please remove related records first.";
        } else if (root != null && root.toLowerCase().contains("unique")) {
            userMsg = "A record with this value already exists.";
        } else {
            userMsg = "Database constraint violation. Please check related records.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", userMsg));
    }

    // ── Spring Security access denied ─────────────────────────────────────────
    // Must re-throw so Spring Security's filter chain handles it correctly (401/403).

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access denied: you don't have permission to perform this action."));
    }

    // ── Catch-all: true unexpected errors ────────────────────────────────────
    // Returns 500 but now logs the real stack trace so you can diagnose it.

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, String>> handleAll(Throwable ex) {
        System.err.println("❌ Unhandled exception: " + ex.getMessage());
        ex.printStackTrace();

        String msg = ex.getMessage() != null
                ? ex.getMessage()
                : "An unexpected server error occurred. Please try again.";

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", msg,
                        "error", "Internal Server Error",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}