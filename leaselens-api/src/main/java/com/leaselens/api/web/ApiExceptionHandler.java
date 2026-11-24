package com.leaselens.api.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.leaselens.api.service.ApiExceptions.DatasetRejectedException;
import com.leaselens.api.service.ApiExceptions.NoDatasetException;
import com.leaselens.api.service.ApiExceptions.NotFoundException;

/**
 * Maps exceptions to RFC 7807 problem responses, so the frontend always receives a readable
 * explanation. Framework errors (bad parameter types, missing parameters, unsupported methods,
 * oversized uploads) are handled by {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(NoDatasetException.class)
    public ProblemDetail noDataset(NoDatasetException ex) {
        return problem(HttpStatus.CONFLICT, "No dataset loaded", ex.getMessage());
    }

    @ExceptionHandler(DatasetRejectedException.class)
    public ProblemDetail rejected(DatasetRejectedException ex) {
        ProblemDetail p = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Dataset failed validation", ex.getMessage());
        p.setProperty("validation", ex.report());
        return p;
    }

    @ExceptionHandler(com.leaselens.api.service.ApiExceptions.BusyException.class)
    public ProblemDetail busy(com.leaselens.api.service.ApiExceptions.BusyException ex) {
        return problem(HttpStatus.CONFLICT, "Busy", ex.getMessage());
    }

    @ExceptionHandler(ArithmeticException.class)
    public ProblemDetail overflow(ArithmeticException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Calculation out of range",
                "A total exceeded the supported range, so no result was returned (" + ex.getMessage() + ").");
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail unexpected(RuntimeException ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                "The request could not be completed. No partial result was returned.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setType(URI.create("about:blank"));
        return p;
    }
}
