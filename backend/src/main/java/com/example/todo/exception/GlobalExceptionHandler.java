package com.example.todo.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ProblemDetails.of(ex.getStatus(), ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex; // let Spring Security's 401/403 handlers produce the response
        }
        log.error("Unhandled exception", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(ProblemDetails.of(status, "INTERNAL_ERROR", "An unexpected error occurred."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    /** A value that cannot be parsed (such as an unknown enum constant) is reported like any other invalid field. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (!(ex.getCause() instanceof InvalidFormatException invalid) || invalid.getPath().isEmpty()) {
            return super.handleHttpMessageNotReadable(ex, headers, status, request);
        }
        String field = invalid.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.");
        problem.setProperty("errors", List.of(Map.of("field", field, "message", "has an invalid value")));
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body == null) {
            HttpStatus status = HttpStatus.valueOf(statusCode.value());
            String code = status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : status.name();
            body = ProblemDetails.of(status, code, status.is5xxServerError()
                    ? "An unexpected error occurred." : status.getReasonPhrase());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
