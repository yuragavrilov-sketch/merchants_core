package ru.copperside.core.api;

import ru.copperside.core.application.MerchantNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RFC 9457 ProblemDetail-обёртка для всех исключений REST-слоя (см. ADR-0001).
 * Контент-тайп — application/problem+json.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ProblemEnvelope> handleNotFound(MerchantNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND", "merchant-not-found", "Merchant not found", ex.getMessage(), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemEnvelope> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            fields.put(v.getPropertyPath().toString(), v.getMessage());
        }
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation-error", "Validation failed", ex.getMessage(), Map.of("fields", fields));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemEnvelope> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation-error", "Validation failed", ex.getMessage(), Map.of("fields", fields));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ProblemEnvelope> handleBadRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "bad-request", "Bad request", ex.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemEnvelope> handleNoResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "not-found", "Not Found", "Resource not found: " + ex.getResourcePath(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemEnvelope> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "internal", "Internal server error", null, null);
    }

    private static ResponseEntity<ProblemEnvelope> problem(
            HttpStatus status,
            String code,
            String typeSuffix,
            String title,
            String message,
            Map<String, Object> details
    ) {
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        ProblemDetail detail = new ProblemDetail(
                TYPE_BASE + typeSuffix,
                title,
                status.value(),
                code,
                message,
                details,
                traceId
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemEnvelope.of(detail));
    }
}
