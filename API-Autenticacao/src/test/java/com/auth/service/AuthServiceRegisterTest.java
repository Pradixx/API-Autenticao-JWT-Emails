package com.auth.service;

import com.auth.dto.MessageResponse;
import com.auth.dto.RegisterRequest;
import com.auth.entity.EmailVerificationToken;
import com.auth.entity.User;
import com.auth.exception.UserAlreadyExistsException;
import com.auth.repository.EmailVerificationTokenRepository;
import com.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - register()")
class AuthServiceRegisterTest {

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

    private RegisterRequest buildRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Diego");
        request.setLastName("Prado");
        request.setEmail("diego@gmail.com");
        request.setPassword("Senha@123");
        return request;
    }

    @Test
    @DisplayName("deve registrar usuário com sucesso e retornar mensagem")
    void deveRegistrarComSucesso() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        MessageResponse response = authService.register(buildRequest());

        assertThat(response.getMessage())
                .contains("Cadastro realizado com sucesso");
    }

    @Test
    @DisplayName("deve salvar usuário com enabled = false")
    void deveSalvarUsuarioDesabilitado() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(buildRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("deve salvar senha como hash, não em texto puro")
    void deveSalvarSenhaHasheada() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("$2a$hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(buildRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPassword())
                .isEqualTo("$2a$hash-bcrypt")
                .isNotEqualTo("Senha@123");
    }

    @Test
    @DisplayName("deve normalizar e-mail para minúsculas ao salvar")
    void deveNormalizarEmailParaMinusculas() {
        RegisterRequest request = buildRequest();
        request.setEmail("DIEGO@GMAIL.COM");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("diego@gmail.com");
    }

    @Test
    @DisplayName("deve gerar e salvar token de verificação")
    void deveSalvarTokenDeVerificacao() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(buildRequest());

        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("deve enviar e-mail de verificação após cadastro")
    void deveEnviarEmailDeVerificacao() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(buildRequest());

        verify(emailService, times(1))
                .sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("deve lançar UserAlreadyExistsException se e-mail já estiver cadastrado")
    void deveLancarExcecaoParaEmailDuplicado() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRequest()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("diego@gmail.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException para e-mail de domínio descartável")
    void deveLancarExcecaoParaDominioDescartavel() {
        doThrow(new IllegalArgumentException("E-mails de domínios temporários ou descartáveis não são permitidos."))
                .when(emailValidationService).validate(anyString());

        assertThatThrownBy(() -> authService.register(buildRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descartáveis");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("não deve salvar usuário se validação de e-mail falhar")
    void naoDeveSalvarSeValidacaoFalhar() {
        doThrow(new IllegalArgumentException("Formato de e-mail inválido."))
                .when(emailValidationService).validate(anyString());

        assertThatThrownBy(() -> authService.register(buildRequest()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

}
