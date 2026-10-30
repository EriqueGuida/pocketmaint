package br.com.aberturaordensservico.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresInSeconds,
        UserSummaryDto user
) {}
