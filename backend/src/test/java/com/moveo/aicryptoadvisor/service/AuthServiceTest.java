package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moveo.aicryptoadvisor.dto.request.RegisterRequest;
import com.moveo.aicryptoadvisor.entity.User;
import com.moveo.aicryptoadvisor.exception.EmailTakenException;
import com.moveo.aicryptoadvisor.repository.UserRepository;
import com.moveo.aicryptoadvisor.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService() {
        return new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_duplicateEmail_throwsEmailTaken() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Jane Doe", "Str0ngPass");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(request))
                .isInstanceOf(EmailTakenException.class);
    }

    @Test
    void register_newEmail_hashesPasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Jane Doe", "Str0ngPass");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), any())).thenReturn("fake-jwt");

        authService().register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Str0ngPass");
        assertThat(passwordEncoder.matches("Str0ngPass", savedUser.getPasswordHash())).isTrue();
    }
}
