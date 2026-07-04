package com.orque.crm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Auth failures delegated to OPAC — return 401 not 500. */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class, LockedException.class})
    public ResponseEntity<Map<String, Object>> handleAuthException(Exception ex) {
        return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * CRM delegates credential validation to OPAC. OPAC returns a structured error
     * which AuthServiceImpl throws as RuntimeException with the OPAC message.
     * Detect auth-context messages so the client gets a 401 instead of 500.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Unexpected error";
        boolean isAuthFailure = msg.contains("Invalid credentials")
                || msg.contains("invalid credentials")
                || msg.contains("not found")
                || msg.contains("disabled")
                || msg.contains("suspended")
                || msg.contains("SSO")
                || msg.contains("token");

        if (isAuthFailure) {
            return errorResponse(HttpStatus.UNAUTHORIZED, msg);
        }
        log.error("Unhandled RuntimeException", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
    }

    /** Validation errors from @Valid — return 400 with field details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return errorResponse(HttpStatus.BAD_REQUEST, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("Unhandled Exception", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}