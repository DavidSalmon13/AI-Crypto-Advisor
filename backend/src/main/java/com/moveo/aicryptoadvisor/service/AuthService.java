package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.dto.request.LoginRequest;
import com.moveo.aicryptoadvisor.dto.request.RegisterRequest;
import com.moveo.aicryptoadvisor.dto.response.AuthResponse;
import com.moveo.aicryptoadvisor.entity.User;
import com.moveo.aicryptoadvisor.exception.EmailTakenException;
import com.moveo.aicryptoadvisor.exception.InvalidCredentialsException;
import com.moveo.aicryptoadvisor.repository.UserRepository;
import com.moveo.aicryptoadvisor.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailTakenException(request.email());
        }

        User user = new User(request.email(), request.name(), passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, new AuthResponse.UserResponse(user.getId(), user.getEmail(), user.getName()));
    }
}
