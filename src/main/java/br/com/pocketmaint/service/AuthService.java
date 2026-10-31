package br.com.pocketmaint.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import br.com.pocketmaint.dto.LoginRequest;
import br.com.pocketmaint.dto.LoginResponse;
import br.com.pocketmaint.dto.UserMeResponse;
import br.com.pocketmaint.dto.UserSummaryDto;
import br.com.pocketmaint.exception.BusinessValidationException;
import br.com.pocketmaint.model.User;
import br.com.pocketmaint.repository.UserRepository;
import br.com.pocketmaint.security.CustomUserDetails;
import br.com.pocketmaint.security.JwtService;

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
                            request.username().trim(),
                            request.password()
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

            return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), userSummary);

        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Usuário inexistente ou senha inválida");
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
