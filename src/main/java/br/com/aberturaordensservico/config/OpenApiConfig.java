package br.com.aberturaordensservico.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PocketMaint API")
                        .version("1.0.0")
                        .description("""
                                ### API RESTful de Gestão e Execução de Ordens de Serviço Industrial
                                
                                Desenvolvida para comunicação entre a retaguarda industrial e os terminais portáteis **M5Stack CardPuter ADV**.
                                
                                - **Autenticação:** Baseada em tokens Bearer JWT (HS256) com validade padrão de 8 horas.
                                - **Controle de Acesso:** Baseado em Perfis (RBAC): `TECHNICIAN`, `SUPERVISOR`, `ADMIN`.
                                - **Proteção Anti-IDOR / Anti-BOLA:** Validação estrita de posse da ordem no nível de serviço e controller.
                                - **Tratamento de Erros:** Formatado no padrão canônico **RFC 9457 (Problem Details for HTTP APIs)**.
                                - **Idempotência e Auditoria:** Suporte ao cabeçalho `Idempotency-Key` e persistência transacional com `AuditEvent`.
                                """)
                        .contact(new Contact()
                                .name("PocketMaint Maintenance Engineering Team")
                                .url("https://github.com/EriqueGuida/pocketmaint"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT retornado pelo endpoint /api/v1/auth/login")));
    }
}
