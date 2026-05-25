package com.auth.service;

import com.auth.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final EmailVerificationTokenRepository tokenRepository;

    // Executa todo dia às 03:00 da manhã
    // Cron: segundos minutos horas dia-do-mês mês dia-da-semana
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.info("Iniciando limpeza de tokens de verificação expirados...");

        int deleted = tokenRepository.deleteAllExpiredBefore(LocalDateTime.now());

        log.info("Limpeza concluída: {} token(s) expirado(s) removido(s).", deleted);
    }

}
