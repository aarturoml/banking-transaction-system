package com.example.banking.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        return response(status, error(status, exception.getReason(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return response(
                HttpStatus.BAD_REQUEST,
                error(HttpStatus.BAD_REQUEST, "Request validation failed", fields)
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException exception) {
        return response(
                HttpStatus.BAD_REQUEST,
                error(HttpStatus.BAD_REQUEST,
                        "Required header is missing: " + exception.getHeaderName(), Map.of())
        );
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials() {
        return response(
                HttpStatus.UNAUTHORIZED,
                error(HttpStatus.UNAUTHORIZED, "Invalid credentials", Map.of())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", Map.of())
        );
    }

    private ResponseEntity<ApiError> response(HttpStatus status, ApiError error) {
        return ResponseEntity.status(status).body(error);
    }

    private ApiError error(HttpStatus status, String message, Map<String, String> fields) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message == null ? status.getReasonPhrase() : message,
                fields
        );
    }
}
