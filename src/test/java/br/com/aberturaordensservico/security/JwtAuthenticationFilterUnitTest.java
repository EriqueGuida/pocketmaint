package br.com.aberturaordensservico.security;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterUnitTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private SecurityConfig securityConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        securityConfig = new SecurityConfig(filter, userDetailsService, null);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("JwtAuthenticationFilter deve capturar ExpiredJwtException e sinalizar a requisição")
    void shouldSignalRequestWhenTokenIsExpired() throws ServletException, IOException {
        String expiredToken = "header.payload.signature";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtService.extractUsername(expiredToken)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(Boolean.TRUE, request.getAttribute(JwtAuthenticationFilter.JWT_EXPIRED_ATTRIBUTE));
        assertEquals(Boolean.TRUE, request.getAttribute("token_expired"));
    }

    @Test
    @DisplayName("AuthenticationEntryPoint deve retornar AUTH_TOKEN_EXPIRED quando o token estiver expirado")
    void shouldReturnAuthTokenExpiredWhenSignalPresent() throws Exception {
        AuthenticationEntryPoint entryPoint = securityConfig.authenticationEntryPoint();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/10");
        request.setAttribute(JwtAuthenticationFilter.JWT_EXPIRED_ATTRIBUTE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationException("Full authentication required") {};

        entryPoint.commence(request, response, authException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        String jsonResponse = response.getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);

        assertEquals(401, root.get("status").asInt());
        assertEquals("AUTH_TOKEN_EXPIRED", root.get("errorCode").asText());
        assertEquals("Token de autenticação expirado", root.get("title").asText());
        assertEquals("/api/v1/orders/10", root.get("path").asText());
        assertNotNull(root.get("timestamp"));
        assertFalse(root.get("timestamp").asText().isBlank());
        assertTrue(root.has("fieldErrors"));
    }

    @Test
    @DisplayName("AuthenticationEntryPoint deve retornar AUTH_REQUIRED quando o token for ausente ou malformado")
    void shouldReturnAuthRequiredWhenTokenMissingOrMalformed() throws Exception {
        AuthenticationEntryPoint entryPoint = securityConfig.authenticationEntryPoint();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/orders");
        // Não define jwt_expired
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationException("Full authentication required") {};

        entryPoint.commence(request, response, authException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        String jsonResponse = response.getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);

        assertEquals(401, root.get("status").asInt());
        assertEquals("AUTH_REQUIRED", root.get("errorCode").asText());
        assertEquals("Autenticação necessária", root.get("title").asText());
        assertEquals("/api/v1/me/orders", root.get("path").asText());
        assertNotNull(root.get("timestamp"));
        assertFalse(root.get("timestamp").asText().isBlank());
    }
}
