package br.com.aberturaordensservico.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desativa CSRF para API REST stateless
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Desativa formulário HTML web e Basic Auth de fábrica
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Sessão stateless via token JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Tratamento de falha de autenticação (retorna 401 sem redirecionar para tela HTML)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"errorCode\":\"AUTH_REQUIRED\",\"title\":\"Autenticação necessária\",\"userMessage\":\"Token Bearer ausente ou inválido.\",\"path\":\"" + request.getRequestURI() + "\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"status\":403,\"errorCode\":\"ACCESS_DENIED_INSUFFICIENT_ROLE\",\"title\":\"Acesso negado\",\"userMessage\":\"Perfil de usuário sem permissão para este recurso.\",\"path\":\"" + request.getRequestURI() + "\"}"
                            );
                        })
                )

                // Regras de autorização por endpoint
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Endpoints de mutação de setores e equipamentos restritos a ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/sectors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/sectors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/sectors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/sectors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/equipments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/equipments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/equipments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/equipments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/sectors/**").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/equipments/**").hasAnyRole("SUPERVISOR", "ADMIN")

                        // Endpoints de criação/atribuição/cancelamento de OS para SUPERVISOR e ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/assign").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/cancel").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*/audit").hasAnyRole("SUPERVISOR", "ADMIN")

                        // Início e conclusão de atendimento exclusivos para TECHNICIAN
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/start").hasRole("TECHNICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/complete").hasRole("TECHNICIAN")

                        // Demais rotas exigem autenticação
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Idempotency-Key", "X-Origin-Channel", "X-Actor-Technician-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
