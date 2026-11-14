package com.hotelportal.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** The one error shape every endpoint returns. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {
    public record FieldErrorDetail(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String code, String message, String path,
                              List<FieldErrorDetail> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path,
                fieldErrors == null ? List.of() : fieldErrors);
    }
}
