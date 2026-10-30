package br.com.aberturaordensservico.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String title,
        String userMessage,
        String path,
        List<FieldErrorDto> fieldErrors
) {
    public ApiErrorResponse(int status, String errorCode, String title, String userMessage, String path) {
        this(Instant.now(), status, errorCode, title, userMessage, path, Collections.emptyList());
    }

    public ApiErrorResponse(int status, String errorCode, String title, String userMessage, String path, List<FieldErrorDto> fieldErrors) {
        this(Instant.now(), status, errorCode, title, userMessage, path, fieldErrors != null ? fieldErrors : Collections.emptyList());
    }
}
