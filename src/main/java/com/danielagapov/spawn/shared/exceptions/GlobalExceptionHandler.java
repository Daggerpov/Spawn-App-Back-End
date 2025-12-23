package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private ILogger logger;

    /**
     * Handle authentication-related exceptions
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        
        // Log the full error internally with unique ID
        logger.error("Authentication error [" + errorId + "]: " + ex.getMessage());
        
        // Return generic message to prevent user enumeration
        Map<String, Object> response = createErrorResponse(
            "AUTHENTICATION_FAILED",
            "Invalid credentials",
            HttpStatus.UNAUTHORIZED,
            errorId
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(BaseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBaseNotFoundException(BaseNotFoundException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        
        logger.warn("Entity not found [" + errorId + "]: " + ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(
            "RESOURCE_NOT_FOUND",
            "The requested resource was not found",
            HttpStatus.NOT_FOUND,
            errorId
        );
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle save operation exceptions
     */
    @ExceptionHandler(BaseSaveException.class)
    public ResponseEntity<Map<String, Object>> handleBaseSaveException(BaseSaveException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        
        logger.error("Save operation failed [" + errorId + "]: " + ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(
            "OPERATION_FAILED",
            "Operation could not be completed",
            HttpStatus.INTERNAL_SERVER_ERROR,
            errorId
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle security exceptions
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        
        logger.error("Security violation [" + errorId + "]: " + ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(
            "SECURITY_VIOLATION",
            "Security policy violation",
            HttpStatus.FORBIDDEN,
            errorId
        );
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle illegal argument exceptions (validation errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        
        logger.warn("Validation error [" + errorId + "]: " + ex.getMessage());
        
        // For validation errors, we can be more specific but still safe
        String sanitizedMessage = sanitizeValidationMessage(ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(
            "VALIDATION_ERROR",
            sanitizedMessage,
            HttpStatus.BAD_REQUEST,
            errorId
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        
        // Log full stack trace for debugging
        logger.error("Unexpected error [" + errorId + "]: " + ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR,
            errorId
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates a standardized error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, HttpStatus status, String errorId) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("errorId", errorId); // For support purposes
        
        return response;
    }

    /**
     * Sanitizes validation messages to prevent information leakage
     */
    private String sanitizeValidationMessage(String message) {
        if (message == null) {
            return "Invalid input provided";
        }
        
        // Remove any potential sensitive information patterns
        String sanitized = message.toLowerCase();
        
        // Check for potentially sensitive patterns and replace with generic messages
        if (sanitized.contains("sql") || sanitized.contains("database") || sanitized.contains("constraint")) {
            return "Invalid input provided";
        }
        
        if (sanitized.contains("file") && (sanitized.contains("size") || sanitized.contains("type"))) {
            return message; // File validation messages are generally safe to show
        }
        
        if (sanitized.contains("password") && sanitized.contains("requirement")) {
            return message; // Password requirement messages are safe
        }
        
        // For other validation messages, return as-is if they seem safe
        if (message.length() < 100 && !message.contains("Exception") && !message.contains("Error")) {
            return message;
        }
        
        return "Invalid input provided";
    }
}
