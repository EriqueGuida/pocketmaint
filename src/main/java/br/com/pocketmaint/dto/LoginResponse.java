package br.com.pocketmaint.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresInSeconds,
        UserSummaryDto user
) {}
