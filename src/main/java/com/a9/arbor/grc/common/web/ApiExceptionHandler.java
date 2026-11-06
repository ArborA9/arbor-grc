package com.a9.arbor.grc.common.web;

import com.a9.arbor.grc.catalog.excel.ImportValidationException;
import com.a9.arbor.grc.catalog.service.TenantAlreadyExistsException;
import com.a9.arbor.grc.modules.iam.service.InvalidCredentialsException;
import com.a9.arbor.grc.modules.iam.service.UserAlreadyExistsException;
import com.a9.arbor.grc.tenancy.UnknownTenantException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Maps common exceptions to RFC-7807 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ProblemDetail handleTenantConflict(TenantAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserConflict(UserAlreadyExistsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setProperty("errors", List.of(Map.of("field", "email", "message", e.getMessage())));
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(UnknownTenantException.class)
    public ProblemDetail handleUnknownTenant(UnknownTenantException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("field", fe.getField());
                    row.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value");
                    return row;
                })
                .toList();
        String detail = errors.stream()
                .map(err -> err.get("field") + ": " + err.get("message"))
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ImportValidationException.class)
    public ProblemDetail handleImportValidation(ImportValidationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setProperty("errors", e.getErrors());
        return problem;
    }
}
