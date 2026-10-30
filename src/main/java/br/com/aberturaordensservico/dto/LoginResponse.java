package br.com.aberturaordensservico.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private UserSummaryDto user;

    public LoginResponse(String accessToken, long expiresInSeconds, UserSummaryDto user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }
}
