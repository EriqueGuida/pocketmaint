package br.com.aberturaordensservico.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Endpoint público para checagem de saúde e conectividade do backend (EP-001)")
public class HealthController {

    @GetMapping
    @Operation(summary = "EP-001: Verificação de Saúde do Serviço", description = "Permite checar a disponibilidade do backend e sincronizar horário")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(new HealthResponse(
                "UP",
                "pocketmaint-api",
                "1.0.0",
                Instant.now()
        ));
    }
}
