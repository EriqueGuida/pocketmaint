package br.com.pocketmaint.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password,

        String clientChannel
) {
    public LoginRequest(String username, String password) {
        this(username, password, null);
    }
}
