# API de Autenticação JWT com Verificação de E-mail

API REST para autenticação completa com JWT e verificação de e-mail, desenvolvida em Java com Spring Boot.

## 🚀 Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Spring Security | 6.x |
| JJWT | 0.12.6 |
| MySQL | 8.x |
| Lombok | - |

---

## ⚙️ Configuração do Ambiente

### Pré-requisitos
- Java 21
- Maven 3.8+
- MySQL 8.x rodando localmente

### 1. Clone o repositório
```bash
git clone <URL_DO_REPOSITÓRIO>
cd API-Autenticacao
```

### 2. Crie o arquivo `.env`
Copie o `.env.example` e preencha com seus valores:
```bash
cp .env.example .env
```

```env
# Banco de Dados
DB_USERNAME=root
DB_PASSWORD=sua_senha

# JWT — mínimo 32 caracteres
JWT_SECRET=sua_chave_secreta_jwt_com_minimo_32_chars

# E-mail (Gmail com Senha de App)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seuemail@gmail.com
MAIL_PASSWORD=sua_senha_de_app

# URL da aplicação
APP_URL=http://localhost:8080
```

> 💡 Para gerar um `JWT_SECRET` seguro:
> ```bash
> openssl rand -base64 32
> ```

> 💡 Para o Gmail, ative **Senhas de app** em:
> *Conta Google → Segurança → Verificação em duas etapas → Senhas de app*

### 3. Execute a aplicação
```bash
mvn spring-boot:run
```

O banco de dados `auth_db` será criado automaticamente pelo Hibernate.

---

## 📡 Endpoints

### Base URL: `http://localhost:8080/api/auth`

---

### 📝 Cadastro
**`POST /register`**

Cria uma nova conta e envia um e-mail de verificação.

**Body:**
```json
{
  "firstName": "Diego",
  "lastName": "Prado",
  "email": "diego@email.com",
  "password": "senha1234"
}
```

**Resposta — `201 Created`:**
```json
{
  "message": "Cadastro realizado com sucesso! Verifique seu e-mail para ativar sua conta."
}
```

**Erros possíveis:**

| Status | Situação |
|---|---|
| `400` | Campos inválidos ou e-mail de domínio descartável |
| `409` | E-mail já cadastrado |

---

### 🔐 Login
**`POST /login`**

Autentica o usuário e retorna o token JWT.

**Body:**
```json
{
  "email": "diego@email.com",
  "password": "senha1234"
}
```

**Resposta — `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "email": "diego@email.com",
  "firstName": "Diego",
  "lastName": "Prado"
}
```

**Erros possíveis:**

| Status | Situação |
|---|---|
| `400` | Campos inválidos |
| `401` | E-mail ou senha incorretos |
| `403` | E-mail ainda não verificado |

---

### ✅ Verificação de E-mail
**`GET /verify-email?token={token}`**

Ativa a conta do usuário a partir do link enviado por e-mail.

**Exemplo:**
```
GET /api/auth/verify-email?token=3f2a1b4c-...
```

**Resposta — `200 OK`:**
```json
{
  "message": "E-mail verificado com sucesso! Sua conta está ativa."
}
```

**Erros possíveis:**

| Status | Situação |
|---|---|
| `400` | Token inválido |
| `400` | Token já utilizado |
| `400` | Token expirado (solicite um novo) |

---

### 🔁 Reenvio de Verificação
**`POST /resend-verification?email={email}`**

Reenvia o e-mail de verificação para contas ainda não ativadas.

**Exemplo:**
```
POST /api/auth/resend-verification?email=diego@email.com
```

**Resposta — `200 OK`:**
```json
{
  "message": "Novo e-mail de verificação enviado. Verifique sua caixa de entrada."
}
```

**Erros possíveis:**

| Status | Situação |
|---|---|
| `404` | E-mail não encontrado |

---

## 🔒 Usando o Token JWT

Após o login, inclua o token no header de todas as requisições protegidas:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

O token expira em **24 horas**.

---

## 🔄 Fluxo Completo

```
[Cadastro]
POST /register → e-mail enviado → usuário clica no link

[Verificação]
GET /verify-email?token=xxx → conta ativada

[Login]
POST /login → retorna JWT

[Acesso protegido]
GET /qualquer-rota → Authorization: Bearer <token>
```

---

## 🗄️ Estrutura do Banco de Dados

O Hibernate cria as tabelas automaticamente no primeiro boot:

| Tabela | Descrição |
|---|---|
| `users` | Dados dos usuários cadastrados |
| `email_verification_tokens` | Tokens de verificação de e-mail |

---

## 📁 Estrutura do Projeto

```
src/main/java/com/auth/
├── config/         → SecurityConfig, AsyncConfig
├── controller/     → AuthController
├── dto/            → RegisterRequest, LoginRequest, AuthResponse, MessageResponse, ErrorResponse
├── entity/         → User, Role, EmailVerificationToken
├── exception/      → Exceções customizadas e GlobalExceptionHandler
├── filter/         → JwtAuthenticationFilter
├── repository/     → UserRepository, EmailVerificationTokenRepository
└── service/        → AuthService, JwtService, EmailService,
                       EmailValidationService, UserDetailsServiceImpl
```
