package com.auth.service;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.entity.User;
import com.auth.exception.EmailNotVerifiedException;
import com.auth.repository.EmailVerificationTokenRepository;
import com.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - login()")
class AuthServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private EmailService emailService;
    @Mock private EmailValidationService emailValidationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationTokenExpiration", 86400000L);
    }

    private LoginRequest buildRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("diego@gmail.com");
        request.setPassword("Senha@123");
        return request;
    }

    private User buildUsuarioAtivo() {
        return User.builder()
                .firstName("Diego")
                .lastName("Prado")
                .email("diego@gmail.com")
                .password("$2a$hash-bcrypt")
                .enabled(true)
                .build();
    }

    private UserDetails buildUserDetails() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("diego@gmail.com")
                .password("$2a$hash-bcrypt")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("deve retornar AuthResponse com token ao fazer login com sucesso")
    void deveRetornarAuthResponseComToken() {
        when(userRepository.findByEmail("diego@gmail.com"))
                .thenReturn(Optional.of(buildUsuarioAtivo()));
        when(userDetailsService.loadUserByUsername("diego@gmail.com"))
                .thenReturn(buildUserDetails());
        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn("jwt-token-gerado");

        AuthResponse response = authService.login(buildRequest());

        assertThat(response.getToken()).isEqualTo("jwt-token-gerado");
    }

    @Test
    @DisplayName("deve retornar dados corretos do usuário no AuthResponse")
    void deveRetornarDadosCorretosdoUsuario() {
        when(userRepository.findByEmail("diego@gmail.com"))
                .thenReturn(Optional.of(buildUsuarioAtivo()));
        when(userDetailsService.loadUserByUsername("diego@gmail.com"))
                .thenReturn(buildUserDetails());
        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn("jwt-token-gerado");

        AuthResponse response = authService.login(buildRequest());

        assertThat(response.getEmail()).isEqualTo("diego@gmail.com");
        assertThat(response.getFirstName()).isEqualTo("Diego");
        assertThat(response.getLastName()).isEqualTo("Prado");
        assertThat(response.getType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("deve chamar AuthenticationManager para validar credenciais")
    void deveChamarAuthenticationManager() {
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(buildUsuarioAtivo()));
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(buildUserDetails());
        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn("jwt-token-gerado");

        authService.login(buildRequest());

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
    }

    @Test
    @DisplayName("deve lançar BadCredentialsException para credenciais inválidas")
    void deveLancarExcecaoParaCredenciaisInvalidas() {
        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(buildRequest()))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("deve lançar EmailNotVerifiedException se conta não estiver ativa")
    void deveLancarExcecaoParaEmailNaoVerificado() {
        User usuarioInativo = buildUsuarioAtivo();
        usuarioInativo.setEnabled(false);

        when(userRepository.findByEmail("diego@gmail.com"))
                .thenReturn(Optional.of(usuarioInativo));

        assertThatThrownBy(() -> authService.login(buildRequest()))
                .isInstanceOf(EmailNotVerifiedException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("não deve gerar token se e-mail não estiver verificado")
    void naoDeveGerarTokenParaEmailNaoVerificado() {
        User usuarioInativo = buildUsuarioAtivo();
        usuarioInativo.setEnabled(false);

        when(userRepository.findByEmail("diego@gmail.com"))
                .thenReturn(Optional.of(usuarioInativo));

        assertThatThrownBy(() -> authService.login(buildRequest()))
                .isInstanceOf(EmailNotVerifiedException.class);

        verifyNoInteractions(jwtService);
    }

}
