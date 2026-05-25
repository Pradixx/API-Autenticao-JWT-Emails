package com.auth.service;

import com.auth.dto.MessageResponse;
import com.auth.entity.EmailVerificationToken;
import com.auth.entity.User;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - resendVerification()")
class AuthServiceResendVerificationTest {

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

    private User buildUsuarioInativo() {
        return User.builder()
                .firstName("Diego")
                .lastName("Prado")
                .email("diego@gmail.com")
                .password("$2a$hash")
                .enabled(false)
                .build();
    }

    private User buildUsuarioAtivo() {
        return User.builder()
                .firstName("Diego")
                .lastName("Prado")
                .email("diego@gmail.com")
                .password("$2a$hash")
                .enabled(true)
                .build();
    }

    private EmailVerificationToken buildTokenExistente(User user) {
        return EmailVerificationToken.builder()
                .token("token-antigo-uuid")
                .user(user)
                .used(false)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    @DisplayName("deve enviar novo e-mail para usuário inativo sem token anterior")
    void deveEnviarEmailParaUsuarioInativoSemTokenAnterior() {
        User user = buildUsuarioInativo();

        when(userRepository.findByEmail("diego@gmail.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.resendVerification("diego@gmail.com");

        verify(emailService, times(1))
                .sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("deve deletar token anterior antes de gerar novo")
    void deveDeletarTokenAnteriorAntesDeGerarNovo() {
        User user = buildUsuarioInativo();
        EmailVerificationToken tokenAntigo = buildTokenExistente(user);

        when(userRepository.findByEmail("diego@gmail.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(tokenAntigo));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.resendVerification("diego@gmail.com");

        verify(tokenRepository).delete(tokenAntigo);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("deve salvar novo token após deletar o anterior")
    void deveSalvarNovoTokenAposDeletear() {
        User user = buildUsuarioInativo();
        EmailVerificationToken tokenAntigo = buildTokenExistente(user);

        when(userRepository.findByEmail("diego@gmail.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(tokenAntigo));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.resendVerification("diego@gmail.com");

        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("deve retornar mensagem genérica para e-mail não cadastrado")
    void deveRetornarMensagemGenericaParaEmailNaoCadastrado() {
        when(userRepository.findByEmail("naocadastrado@gmail.com"))
                .thenReturn(Optional.empty());

        MessageResponse response = authService.resendVerification("naocadastrado@gmail.com");

        assertThat(response.getMessage()).contains("Se este e-mail estiver cadastrado");
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("deve retornar mensagem genérica para usuário já ativo sem enviar e-mail")
    void deveRetornarMensagemGenericaParaUsuarioJaAtivo() {
        when(userRepository.findByEmail("diego@gmail.com"))
                .thenReturn(Optional.of(buildUsuarioAtivo()));

        MessageResponse response = authService.resendVerification("diego@gmail.com");

        assertThat(response.getMessage()).contains("Se este e-mail estiver cadastrado");
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("não deve interagir com tokenRepository se e-mail não estiver cadastrado")
    void naoDeveInteragirComTokenRepositoryParaEmailNaoCadastrado() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        authService.resendVerification("naocadastrado@gmail.com");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("deve enviar e-mail exatamente uma vez para usuário inativo")
    void deveEnviarEmailExatamenteUmaVez() {
        User user = buildUsuarioInativo();

        when(userRepository.findByEmail("diego@gmail.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.resendVerification("diego@gmail.com");

        verify(emailService, times(1))
                .sendVerificationEmail(eq("diego@gmail.com"), eq("Diego"), anyString());
    }

}
