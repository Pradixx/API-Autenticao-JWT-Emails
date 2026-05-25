package com.auth.service;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.MessageResponse;
import com.auth.dto.RegisterRequest;
import com.auth.entity.EmailVerificationToken;
import com.auth.entity.User;
import com.auth.exception.EmailNotVerifiedException;
import com.auth.exception.InvalidTokenException;
import com.auth.exception.UserAlreadyExistsException;
import com.auth.exception.UserNotFoundException;
import com.auth.repository.EmailVerificationTokenRepository;
import com.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final EmailValidationService emailValidationService;

    @Value("${application.jwt.verification-token-expiration}")
    private long verificationTokenExpiration;

    // ===== Registro =====

    @Transactional
    public MessageResponse register(RegisterRequest request) {

        // 1. Valida formato e domínio do e-mail
        emailValidationService.validate(request.getEmail());

        // 2. Verifica se o e-mail já está cadastrado
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        // 3. Cria e salva o usuário (desabilitado até verificação do e-mail)
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        log.info("Novo usuário registrado: {}", user.getEmail());

        // 4. Gera e salva o token de verificação
        String token = generateAndSaveVerificationToken(user);

        // 5. Envia o e-mail de verificação de forma assíncrona
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

        return new MessageResponse(
                "Cadastro realizado com sucesso! Verifique seu e-mail para ativar sua conta."
        );
    }

    // ===== Login =====

    public AuthResponse login(LoginRequest request) {

        // 1. Autentica via Spring Security (lança BadCredentialsException se inválido)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Carrega o usuário do banco
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // 3. Verifica se o e-mail foi confirmado
        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException();
        }

        // 4. Gera o token JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        log.info("Login realizado: {}", user.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    // ===== Verificação de E-mail =====

    @Transactional
    public MessageResponse verifyEmail(String token) {

        // 1. Busca o token no banco
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token de verificação inválido."));

        // 2. Verifica se já foi utilizado
        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Este token já foi utilizado.");
        }

        // 3. Verifica se expirou
        if (verificationToken.isExpired()) {
            throw new InvalidTokenException(
                    "Token expirado. Solicite um novo e-mail de verificação."
            );
        }

        // 4. Ativa o usuário
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // 5. Marca o token como usado
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("E-mail verificado com sucesso: {}", user.getEmail());

        return new MessageResponse("E-mail verificado com sucesso! Sua conta está ativa.");
    }

    // ===== Reenvio de Verificação =====

    @Transactional
    public MessageResponse resendVerification(String email) {

        // Resposta genérica usada em todos os casos para evitar enumeração de e-mails
        final String genericMessage =
                "Se este e-mail estiver cadastrado e não verificado, você receberá um novo link em breve.";

        // 1. Busca o usuário — retorna mensagem genérica se não encontrado (sem revelar existência)
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.debug("Reenvio solicitado para e-mail não cadastrado: {}", email);
            return new MessageResponse(genericMessage);
        }

        // 2. Verifica se já está ativo — retorna mensagem genérica sem revelar o estado
        if (user.isEnabled()) {
            log.debug("Reenvio solicitado para e-mail já verificado: {}", email);
            return new MessageResponse(genericMessage);
        }

        // 3. Remove token anterior, se existir
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // 4. Gera e salva novo token
        String token = generateAndSaveVerificationToken(user);

        // 5. Envia novo e-mail de verificação de forma assíncrona
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

        log.info("Novo e-mail de verificação enviado para: {}", email);

        return new MessageResponse(genericMessage);
    }

    // ===== Método Privado =====

    private String generateAndSaveVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(verificationTokenExpiration / 1000))
                .build();

        tokenRepository.save(verificationToken);
        return token;
    }

}
