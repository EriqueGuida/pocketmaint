package br.com.aberturaordensservico.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.LoginRequest;
import br.com.aberturaordensservico.dto.LoginResponse;
import br.com.aberturaordensservico.dto.UserMeResponse;
import br.com.aberturaordensservico.security.CustomUserDetails;
import br.com.aberturaordensservico.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * EP-002: Autenticação e Emissão de Credencial de Sessão (JWT)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * EP-003: Identificação da Sessão Atual
     */
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getCurrentSession(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserMeResponse response = authService.getSessionDetails(userDetails != null ? userDetails.getUser() : null);
        return ResponseEntity.ok(response);
    }
}
