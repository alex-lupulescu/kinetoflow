package com.kinetoflow.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest; // Can be used for more context if needed

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for the REST API.
 * Catches specific exceptions and formats the error response consistently.
 */
@RestControllerAdvice // Indicates this class advises controllers for exception handling
@Slf4j // Lombok annotation for logging
public class GlobalExceptionHandler {

    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String TIMESTAMP = "timestamp";
    private static final String DETAILS = "details"; // For validation errors

    /**
     * Handles validation errors triggered by @Valid annotation on controller method arguments.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed for request {}: {}", request.getDescription(false) , errors);

        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.BAD_REQUEST.value());
        body.put(ERROR, "Validation Failed");
        body.put(MESSAGE, "Input validation failed. Please check the details.");
        body.put(DETAILS, errors); // Include specific field errors

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles Spring Security authentication errors (e.g., bad credentials during login).
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed for request {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.UNAUTHORIZED.value());
        body.put(ERROR, "Unauthorized");
        // Provide a generic message for security reasons, specific message logged above.
        body.put(MESSAGE, "Authentication failed: Invalid credentials or access denied.");

        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles Spring Security authorization errors (e.g., insufficient permissions for an endpoint).
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied for request {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.FORBIDDEN.value());
        body.put(ERROR, "Forbidden");
        body.put(MESSAGE, "Access Denied: You do not have permission to access this resource.");

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles custom ResourceNotFoundException.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found for request {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.NOT_FOUND.value());
        body.put(ERROR, "Not Found");
        body.put(MESSAGE, ex.getMessage()); // Safe to return the specific message here

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles custom BadRequestException (e.g., invalid state, duplicate resource attempt).
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        log.warn("Bad request for {}: {}", request.getDescription(false), ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.BAD_REQUEST.value());
        body.put(ERROR, "Bad Request");
        body.put(MESSAGE, ex.getMessage()); // Safe to return the specific message

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    /**
     * Handles general, unexpected Runtime Exceptions as a fallback.
     * Returns HTTP 500 Internal Server Error. Logs the full stack trace.
     */
    @ExceptionHandler(Exception.class) // Catch broader Exception as fallback
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(Exception ex, WebRequest request) {
        // Log the full stack trace for unexpected errors
        log.error("An unexpected error occurred processing request {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put(ERROR, "Internal Server Error");
        // Provide a generic message to the client for security/simplicity
        body.put(MESSAGE, "An unexpected error occurred on the server. Please try again later.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.warn("Forbidden action attempt for request {}: {}", request.getDescription(false), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, HttpStatus.FORBIDDEN.value());
        body.put(ERROR, "Forbidden");
        body.put(MESSAGE, ex.getMessage()); // Return the specific reason why it's forbidden
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

}