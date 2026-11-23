package com.alzcare.identity.controllers;

import com.alzcare.identity.exceptions.CaregiverNotFoundException;
import com.alzcare.identity.exceptions.DoctorNotFoundException;
import com.alzcare.identity.exceptions.PatientNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the identity service.
 * Converts exceptions to RFC 7807 ProblemDetail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_URI_BASE = "https://alzcare.com/errors/";

    /**
     * Handles PatientNotFoundException.
     *
     * @param ex the exception
     * @return a ProblemDetail response with 404 status
     */
    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePatientNotFoundException(PatientNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Patient Not Found");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "patient-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles DoctorNotFoundException.
     *
     * @param ex the exception
     * @return a ProblemDetail response with 404 status
     */
    @ExceptionHandler(DoctorNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDoctorNotFoundException(DoctorNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Doctor Not Found");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "doctor-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles CaregiverNotFoundException.
     *
     * @param ex the exception
     * @return a ProblemDetail response with 404 status
     */
    @ExceptionHandler(CaregiverNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCaregiverNotFoundException(CaregiverNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Caregiver Not Found");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "caregiver-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles validation exceptions from @Valid annotation.
     *
     * @param ex the exception
     * @return a ProblemDetail response with 400 status and field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        problemDetail.setDetail("Request validation failed");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "validation-failed"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles IllegalArgumentException (e.g., duplicate Keycloak ID or validation errors).
     *
     * @param ex the exception
     * @return a ProblemDetail response with 400 or 409 status depending on the error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        // Determine status based on message content
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String title = "Bad Request";
        String errorType = "bad-request";
        
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("already exists")) {
                status = HttpStatus.CONFLICT;
                title = "Conflict";
                errorType = "conflict";
            } else if (message.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
                title = "Not Found";
                errorType = "not-found";
            }
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create(ERROR_URI_BASE + errorType));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problemDetail);
    }

    /**
     * Handles malformed path/query parameters (e.g. a non-UUID patient id).
     * Without this handler they fell through to the RuntimeException handler and
     * were reported as a 500 "Profile Creation Failed".
     *
     * @param ex the exception
     * @return a ProblemDetail response with 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()));
        problemDetail.setTitle("Invalid Parameter");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "invalid-parameter"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Handles RuntimeException (e.g., profile creation failures after Keycloak user creation).
     *
     * @param ex the exception
     * @return a ProblemDetail response with 500 status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Profile Creation Failed");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "profile-creation-failed"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    /**
     * Handles all other uncaught exceptions.
     *
     * @param ex the exception
     * @return a ProblemDetail response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create(ERROR_URI_BASE + "internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
