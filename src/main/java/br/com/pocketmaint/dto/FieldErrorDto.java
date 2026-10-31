package br.com.pocketmaint.dto;

public record FieldErrorDto(
        String field,
        String message
) {
}
