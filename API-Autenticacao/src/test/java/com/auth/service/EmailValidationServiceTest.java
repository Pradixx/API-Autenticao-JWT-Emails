package com.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("EmailValidationService")
class EmailValidationServiceTest {

    private EmailValidationService emailValidationService;

    @BeforeEach
    void setUp() {
        emailValidationService = new EmailValidationService();
    }

    // ===== validate() =====

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("deve aceitar e-mail com formato válido")
        void deveAceitarEmailValido() {
            assertThatNoException()
                    .isThrownBy(() -> emailValidationService.validate("usuario@gmail.com"));
        }

        @Test
        @DisplayName("deve aceitar e-mail com subdomínio")
        void deveAceitarEmailComSubdominio() {
            assertThatNoException()
                    .isThrownBy(() -> emailValidationService.validate("usuario@mail.empresa.com.br"));
        }

        @Test
        @DisplayName("deve aceitar e-mail com pontos e caracteres especiais permitidos")
        void deveAceitarEmailComCaracteresEspeciaisPermitidos() {
            assertThatNoException()
                    .isThrownBy(() -> emailValidationService.validate("usuario.nome+tag@outlook.com"));
        }

        @Test
        @DisplayName("deve rejeitar e-mail nulo")
        void deveRejeitarEmailNulo() {
            assertThatThrownBy(() -> emailValidationService.validate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O e-mail não pode ser vazio.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail vazio")
        void deveRejeitarEmailVazio() {
            assertThatThrownBy(() -> emailValidationService.validate("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O e-mail não pode ser vazio.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail sem @")
        void deveRejeitarEmailSemArroba() {
            assertThatThrownBy(() -> emailValidationService.validate("usuariogmail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Formato de e-mail inválido.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail sem domínio")
        void deveRejeitarEmailSemDominio() {
            assertThatThrownBy(() -> emailValidationService.validate("usuario@"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Formato de e-mail inválido.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail sem extensão de domínio")
        void deveRejeitarEmailSemExtensaoDominio() {
            assertThatThrownBy(() -> emailValidationService.validate("usuario@dominio"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Formato de e-mail inválido.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail do domínio mailinator.com")
        void deveRejeitarMailinator() {
            assertThatThrownBy(() -> emailValidationService.validate("teste@mailinator.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mails de domínios temporários ou descartáveis não são permitidos.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail do domínio yopmail.com")
        void deveRejeitarYopmail() {
            assertThatThrownBy(() -> emailValidationService.validate("teste@yopmail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mails de domínios temporários ou descartáveis não são permitidos.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail do domínio guerrillamail.com")
        void deveRejeitarGuerrillamail() {
            assertThatThrownBy(() -> emailValidationService.validate("teste@guerrillamail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mails de domínios temporários ou descartáveis não são permitidos.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail do domínio tempmail.com")
        void deveRejeitarTempmail() {
            assertThatThrownBy(() -> emailValidationService.validate("teste@tempmail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mails de domínios temporários ou descartáveis não são permitidos.");
        }

        @Test
        @DisplayName("deve rejeitar e-mail do domínio trashmail.com")
        void deveRejeitarTrashmail() {
            assertThatThrownBy(() -> emailValidationService.validate("teste@trashmail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mails de domínios temporários ou descartáveis não são permitidos.");
        }

        @Test
        @DisplayName("deve normalizar e-mail em maiúsculas antes de validar")
        void deveNormalizarEmailEmMaiusculas() {
            assertThatNoException()
                    .isThrownBy(() -> emailValidationService.validate("USUARIO@GMAIL.COM"));
        }
    }

    // ===== isValid() =====

    @Nested
    @DisplayName("isValid()")
    class IsValid {

        @Test
        @DisplayName("deve retornar true para e-mail válido")
        void deveRetornarTrueParaEmailValido() {
            assertThat(emailValidationService.isValid("usuario@gmail.com")).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para e-mail nulo")
        void deveRetornarFalseParaEmailNulo() {
            assertThat(emailValidationService.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para formato inválido")
        void deveRetornarFalseParaFormatoInvalido() {
            assertThat(emailValidationService.isValid("nao-e-um-email")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para domínio descartável")
        void deveRetornarFalseParaDominioDescartavel() {
            assertThat(emailValidationService.isValid("teste@mailinator.com")).isFalse();
        }
    }

}
