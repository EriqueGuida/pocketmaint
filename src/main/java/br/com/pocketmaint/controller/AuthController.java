package br.com.pocketmaint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.pocketmaint.dto.LoginRequest;
import br.com.pocketmaint.dto.LoginResponse;
import br.com.pocketmaint.dto.UserMeResponse;
import br.com.pocketmaint.security.CustomUserDetails;
import br.com.pocketmaint.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Autenticação e validação de sessão via Bearer JWT (EP-002 e EP-003)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * EP-002: Autenticação e Emissão de Credencial de Sessão (JWT)
     */
    @PostMapping("/login")
    @Operation(summary = "EP-002: Login e emissão de JWT", description = "Autentica usuário com username e password, gerando Bearer Token com 8 horas de validade")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * EP-003: Identificação da Sessão Atual
     */
    @GetMapping("/me")
    @Operation(summary = "EP-003: Identificação da sessão ativa", description = "Retorna os dados do usuário autenticado no token JWT")
    public ResponseEntity<UserMeResponse> getCurrentSession(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserMeResponse response = authService.getSessionDetails(userDetails != null ? userDetails.getUser() : null);
        return ResponseEntity.ok(response);
    }
}
