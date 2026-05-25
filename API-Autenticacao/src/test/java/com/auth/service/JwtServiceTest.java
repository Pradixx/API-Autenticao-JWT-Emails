package com.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    // Segredo com no mínimo 32 caracteres (exigido pelo HMAC-SHA256)
    private static final String SECRET = "chave-secreta-de-teste-com-minimo-32-caracteres!!";
    private static final long EXPIRATION = 86400000L; // 24 horas em ms

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);

        userDetails = User.builder()
                .username("usuario@teste.com")
                .password("Senha@123")
                .authorities(Collections.emptyList())
                .build();
    }

    // ===== generateToken() =====

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("deve gerar token não nulo e não vazio")
        void deveGerarTokenNaoNulo() {
            String token = jwtService.generateToken(userDetails);
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("deve gerar token com três partes separadas por ponto (formato JWT)")
        void deveGerarTokenNoFormatoJwt() {
            String token = jwtService.generateToken(userDetails);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("deve gerar tokens diferentes para chamadas distintas")
        void deveGerarTokensDiferentes() {
            String token1 = jwtService.generateToken(userDetails);
            String token2 = jwtService.generateToken(userDetails);
            // Tokens diferentes pois o issuedAt varia em ms
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ===== extractEmail() =====

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmail {

        @Test
        @DisplayName("deve extrair o e-mail correto do subject do token")
        void deveExtrairEmailCorreto() {
            String token = jwtService.generateToken(userDetails);
            assertThat(jwtService.extractEmail(token)).isEqualTo("usuario@teste.com");
        }

        @Test
        @DisplayName("deve extrair e-mail correto para usuários diferentes")
        void deveExtrairEmailDeUsuarioDiferente() {
            UserDetails outroUsuario = User.builder()
                    .username("outro@email.com")
                    .password("Senha@123")
                    .authorities(Collections.emptyList())
                    .build();

            String token = jwtService.generateToken(outroUsuario);
            assertThat(jwtService.extractEmail(token)).isEqualTo("outro@email.com");
        }
    }

    // ===== validateToken() =====

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("deve retornar true para token válido do mesmo usuário")
        void deveRetornarTrueParaTokenValido() {
            String token = jwtService.generateToken(userDetails);
            assertThat(jwtService.validateToken(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando token pertence a outro usuário")
        void deveRetornarFalseParaOutroUsuario() {
            String token = jwtService.generateToken(userDetails);

            UserDetails outroUsuario = User.builder()
                    .username("outro@email.com")
                    .password("Senha@123")
                    .authorities(Collections.emptyList())
                    .build();

            assertThat(jwtService.validateToken(token, outroUsuario)).isFalse();
        }

        @Test
        @DisplayName("deve lançar exceção ao validar token expirado")
        void deveLancarExcecaoParaTokenExpirado() {
            // Gera token já expirado (expiration negativo = expirou no passado)
            ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
            String tokenExpirado = jwtService.generateToken(userDetails);

            assertThatThrownBy(() -> jwtService.validateToken(tokenExpirado, userDetails))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve lançar exceção ao validar token com assinatura inválida")
        void deveLancarExcecaoParaAssinaturaInvalida() {
            // Gera token com segredo diferente
            JwtService outroService = new JwtService();
            ReflectionTestUtils.setField(outroService, "secret", "outro-segredo-completamente-diferente-aqui!!");
            ReflectionTestUtils.setField(outroService, "expiration", EXPIRATION);

            String tokenComOutroSegredo = outroService.generateToken(userDetails);

            // Tenta validar com o serviço original (segredo diferente)
            assertThatThrownBy(() -> jwtService.validateToken(tokenComOutroSegredo, userDetails))
                    .isInstanceOf(Exception.class);
        }
    }

}
