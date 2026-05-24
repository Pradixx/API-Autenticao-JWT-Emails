package com.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.url}")
    private String appUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ===== Envio de E-mail de Verificação =====

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Confirme seu cadastro");
            helper.setText(buildVerificationEmailBody(firstName, token), true);

            mailSender.send(message);
            log.info("E-mail de verificação enviado para: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Falha ao enviar e-mail de verificação para: {}", toEmail, e);
        }
    }

    // ===== Template HTML =====

    private String buildVerificationEmailBody(String firstName, String token) {
        String verificationLink = appUrl + "/api/auth/verify-email?token=" + token;

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4; padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                    style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                                    <!-- Cabeçalho -->
                                    <tr>
                                        <td style="background-color:#4F46E5; padding:32px; text-align:center;">
                                            <h1 style="color:#ffffff; margin:0; font-size:24px;">Confirme seu e-mail</h1>
                                        </td>
                                    </tr>

                                    <!-- Corpo -->
                                    <tr>
                                        <td style="padding:40px 32px;">
                                            <p style="color:#374151; font-size:16px; margin:0 0 16px;">
                                                Olá, <strong>%s</strong>!
                                            </p>
                                            <p style="color:#374151; font-size:16px; margin:0 0 24px;">
                                                Obrigado por se cadastrar. Clique no botão abaixo para ativar sua conta:
                                            </p>

                                            <!-- Botão -->
                                            <table cellpadding="0" cellspacing="0" style="margin:0 auto 32px;">
                                                <tr>
                                                    <td style="background-color:#4F46E5; border-radius:6px; padding:14px 28px;">
                                                        <a href="%s"
                                                           style="color:#ffffff; text-decoration:none; font-size:16px; font-weight:bold;">
                                                            Verificar meu e-mail
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="color:#6B7280; font-size:14px; margin:0 0 8px;">
                                                Se o botão não funcionar, copie e cole o link abaixo no navegador:
                                            </p>
                                            <p style="word-break:break-all; font-size:13px; margin:0 0 32px;">
                                                <a href="%s" style="color:#4F46E5;">%s</a>
                                            </p>

                                            <p style="color:#6B7280; font-size:13px; margin:0;">
                                                Este link expira em <strong>24 horas</strong>.
                                                Se você não criou esta conta, ignore este e-mail.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Rodapé -->
                                    <tr>
                                        <td style="background-color:#F9FAFB; padding:24px 32px; text-align:center; border-top:1px solid #E5E7EB;">
                                            <p style="color:#9CA3AF; font-size:12px; margin:0;">
                                                Você está recebendo este e-mail porque se cadastrou em nossa plataforma.
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(firstName, verificationLink, verificationLink, verificationLink);
    }

}
