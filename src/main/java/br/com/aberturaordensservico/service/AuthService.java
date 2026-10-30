package br.com.aberturaordensservico.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import br.com.aberturaordensservico.dto.LoginRequest;
import br.com.aberturaordensservico.dto.LoginResponse;
import br.com.aberturaordensservico.dto.UserMeResponse;
import br.com.aberturaordensservico.dto.UserSummaryDto;
import br.com.aberturaordensservico.exception.BusinessValidationException;
import br.com.aberturaordensservico.model.User;
import br.com.aberturaordensservico.repository.UserRepository;
import br.com.aberturaordensservico.security.CustomUserDetails;
import br.com.aberturaordensservico.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse authenticate(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String token = jwtService.generateToken(user);

            UserSummaryDto userSummary = new UserSummaryDto(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole()
            );

            return new LoginResponse(token, jwtService.getExpirationSeconds(), userSummary);

        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Usuário ou senha inválidos.");
        }
    }

    public UserMeResponse getSessionDetails(User user) {
        if (user == null) {
            throw new BusinessValidationException("Nenhum usuário autenticado na sessão atual.");
        }
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isActive()
        );
    }
}
