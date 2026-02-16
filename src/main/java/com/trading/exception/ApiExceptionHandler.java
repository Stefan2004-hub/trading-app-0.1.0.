package com.trading.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<ApiFieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> new ApiFieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();

        return build(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request,
            violations
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
        BindException ex,
        HttpServletRequest request
    ) {
        List<ApiFieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> new ApiFieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();

        return build(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request,
            violations
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        List<ApiFieldViolation> violations = ex.getConstraintViolations().stream()
            .map(v -> new ApiFieldViolation(v.getPropertyPath().toString(), v.getMessage()))
            .sorted(Comparator.comparing(ApiFieldViolation::field))
            .toList();

        return build(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request,
            violations
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.BAD_REQUEST,
            "Malformed request body",
            request,
            List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
        MissingServletRequestParameterException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.BAD_REQUEST,
            ex.getParameterName() + " is required",
            request,
            List.of(new ApiFieldViolation(ex.getParameterName(), "parameter is required"))
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
        DuplicateEmailException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateUsername(
        DuplicateUsernameException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(OAuthEmailConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleOAuthEmailConflict(
        OAuthEmailConflictException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {
        String message = ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, message, request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(
        Exception ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            request,
            List.of()
        );
    }

    private static ResponseEntity<ApiErrorResponse> build(
        HttpStatus status,
        String message,
        HttpServletRequest request,
        List<ApiFieldViolation> violations
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            violations
        );
        return ResponseEntity.status(status).body(response);
    }
}
