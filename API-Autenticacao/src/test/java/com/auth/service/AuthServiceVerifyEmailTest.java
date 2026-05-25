package com.auth.service;

import com.auth.dto.MessageResponse;
import com.auth.entity.EmailVerificationToken;
import com.auth.entity.User;
import com.auth.exception.InvalidTokenException;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - verifyEmail()")
class AuthServiceVerifyEmailTest {

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

    private User buildUsuario() {
        return User.builder()
                .firstName("Diego")
                .lastName("Prado")
                .email("diego@gmail.com")
                .password("$2a$hash")
                .enabled(false)
                .build();
    }

    private EmailVerificationToken buildTokenValido(User user) {
        return EmailVerificationToken.builder()
                .token("token-uuid-valido")
                .user(user)
                .used(false)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    @Test
    @DisplayName("deve ativar usuário ao verificar token válido")
    void deveAtivarUsuarioComTokenValido() {
        User user = buildUsuario();
        EmailVerificationToken token = buildTokenValido(user);

        when(tokenRepository.findByToken("token-uuid-valido"))
                .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.verifyEmail("token-uuid-valido");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("deve marcar token como usado após verificação")
    void deveMarcarTokenComoUsado() {
        User user = buildUsuario();
        EmailVerificationToken token = buildTokenValido(user);

        when(tokenRepository.findByToken("token-uuid-valido"))
                .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.verifyEmail("token-uuid-valido");

        ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        assertThat(tokenCaptor.getValue().isUsed()).isTrue();
    }

    @Test
    @DisplayName("deve retornar mensagem de sucesso ao verificar token válido")
    void deveRetornarMensagemDeSucesso() {
        User user = buildUsuario();
        EmailVerificationToken token = buildTokenValido(user);

        when(tokenRepository.findByToken("token-uuid-valido"))
                .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        MessageResponse response = authService.verifyEmail("token-uuid-valido");

        assertThat(response.getMessage()).contains("verificado com sucesso");
    }

    @Test
    @DisplayName("deve lançar InvalidTokenException para token inexistente")
    void deveLancarExcecaoParaTokenInexistente() {
        when(tokenRepository.findByToken("token-invalido"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("token-invalido"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("inválido");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar InvalidTokenException para token já utilizado")
    void deveLancarExcecaoParaTokenJaUtilizado() {
        User user = buildUsuario();
        EmailVerificationToken tokenUsado = EmailVerificationToken.builder()
                .token("token-ja-usado")
                .user(user)
                .used(true)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(tokenRepository.findByToken("token-ja-usado"))
                .thenReturn(Optional.of(tokenUsado));

        assertThatThrownBy(() -> authService.verifyEmail("token-ja-usado"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("já foi utilizado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar InvalidTokenException para token expirado")
    void deveLancarExcecaoParaTokenExpirado() {
        User user = buildUsuario();
        EmailVerificationToken tokenExpirado = EmailVerificationToken.builder()
                .token("token-expirado")
                .user(user)
                .used(false)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(tokenRepository.findByToken("token-expirado"))
                .thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> authService.verifyEmail("token-expirado"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expirado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("não deve salvar nada se o token for inválido")
    void naoDeveSalvarNadaComTokenInvalido() {
        when(tokenRepository.findByToken(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("qualquer-token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }

}
