package com.auth.controller;

import com.auth.config.SecurityConfig;
import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.MessageResponse;
import com.auth.dto.RegisterRequest;
import com.auth.exception.EmailNotVerifiedException;
import com.auth.exception.InvalidTokenException;
import com.auth.exception.UserAlreadyExistsException;
import com.auth.filter.JwtAuthenticationFilter;
import com.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
                )
        }
)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ===== POST /api/auth/register =====

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("deve retornar 201 com mensagem para cadastro válido")
        void deveRetornar201ParaCadastroValido() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Diego");
            request.setLastName("Prado");
            request.setEmail("diego@gmail.com");
            request.setPassword("Senha@123");

            when(authService.register(any(RegisterRequest.class)))
                    .thenReturn(new MessageResponse("Cadastro realizado com sucesso!"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Cadastro realizado com sucesso!"));
        }

        @Test
        @DisplayName("deve retornar 400 quando firstName estiver em branco")
        void deveRetornar400ParaFirstNameEmBranco() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("");
            request.setLastName("Prado");
            request.setEmail("diego@gmail.com");
            request.setPassword("Senha@123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando e-mail tiver formato inválido")
        void deveRetornar400ParaEmailInvalido() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Diego");
            request.setLastName("Prado");
            request.setEmail("email-invalido");
            request.setPassword("Senha@123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando senha não atender critérios de complexidade")
        void deveRetornar400ParaSenhaFraca() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Diego");
            request.setLastName("Prado");
            request.setEmail("diego@gmail.com");
            request.setPassword("senhasimples");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando e-mail já estiver cadastrado")
        void deveRetornar409ParaEmailDuplicado() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Diego");
            request.setLastName("Prado");
            request.setEmail("diego@gmail.com");
            request.setPassword("Senha@123");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("diego@gmail.com"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("deve retornar 400 para e-mail de domínio descartável")
        void deveRetornar400ParaDominioDescartavel() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Diego");
            request.setLastName("Prado");
            request.setEmail("diego@mailinator.com");
            request.setPassword("Senha@123");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("E-mails de domínios temporários ou descartáveis não são permitidos."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("E-mails de domínios temporários ou descartáveis não são permitidos."));
        }
    }

    // ===== POST /api/auth/login =====

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("deve retornar 200 com token para login válido")
        void deveRetornar200ComToken() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("diego@gmail.com");
            request.setPassword("Senha@123");

            AuthResponse authResponse = AuthResponse.builder()
                    .token("jwt-token")
                    .email("diego@gmail.com")
                    .firstName("Diego")
                    .lastName("Prado")
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.email").value("diego@gmail.com"));
        }

        @Test
        @DisplayName("deve retornar 400 quando body estiver incompleto")
        void deveRetornar400ParaBodyIncompleto() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("diego@gmail.com");
            // password ausente

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 401 para credenciais inválidas")
        void deveRetornar401ParaCredenciaisInvalidas() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("diego@gmail.com");
            request.setPassword("SenhaErrada@1");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Credenciais inválidas"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos."));
        }

        @Test
        @DisplayName("deve retornar 403 para e-mail não verificado")
        void deveRetornar403ParaEmailNaoVerificado() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("diego@gmail.com");
            request.setPassword("Senha@123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new EmailNotVerifiedException());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    // ===== GET /api/auth/verify-email =====

    @Nested
    @DisplayName("GET /api/auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("deve retornar 200 para token válido")
        void deveRetornar200ParaTokenValido() throws Exception {
            when(authService.verifyEmail("token-valido"))
                    .thenReturn(new MessageResponse("E-mail verificado com sucesso!"));

            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", "token-valido"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("E-mail verificado com sucesso!"));
        }

        @Test
        @DisplayName("deve retornar 400 para token inválido")
        void deveRetornar400ParaTokenInvalido() throws Exception {
            when(authService.verifyEmail("token-invalido"))
                    .thenThrow(new InvalidTokenException("Token de verificação inválido."));

            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", "token-invalido"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Token de verificação inválido."));
        }

        @Test
        @DisplayName("deve retornar 400 para token expirado")
        void deveRetornar400ParaTokenExpirado() throws Exception {
            when(authService.verifyEmail("token-expirado"))
                    .thenThrow(new InvalidTokenException("Token expirado. Solicite um novo e-mail de verificação."));

            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", "token-expirado"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Token expirado. Solicite um novo e-mail de verificação."));
        }
    }

    // ===== POST /api/auth/resend-verification =====

    @Nested
    @DisplayName("POST /api/auth/resend-verification")
    class ResendVerification {

        @Test
        @DisplayName("deve retornar 200 com mensagem genérica")
        void deveRetornar200ComMensagemGenerica() throws Exception {
            when(authService.resendVerification(anyString()))
                    .thenReturn(new MessageResponse("Se este e-mail estiver cadastrado e não verificado, você receberá um novo link em breve."));

            mockMvc.perform(post("/api/auth/resend-verification")
                            .param("email", "diego@gmail.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Se este e-mail estiver cadastrado e não verificado, você receberá um novo link em breve."));
        }

        @Test
        @DisplayName("deve retornar 200 mesmo para e-mail não cadastrado")
        void deveRetornar200MesmoParaEmailNaoCadastrado() throws Exception {
            when(authService.resendVerification(anyString()))
                    .thenReturn(new MessageResponse("Se este e-mail estiver cadastrado e não verificado, você receberá um novo link em breve."));

            mockMvc.perform(post("/api/auth/resend-verification")
                            .param("email", "naocadastrado@gmail.com"))
                    .andExpect(status().isOk());
        }
    }

}
