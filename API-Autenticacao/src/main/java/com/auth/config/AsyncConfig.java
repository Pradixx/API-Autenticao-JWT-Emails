package com.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // @EnableAsync  — envio de e-mail em thread separada sem bloquear o endpoint
    // @EnableScheduling — habilita tasks agendadas com @Scheduled (ex: limpeza de tokens)
}
