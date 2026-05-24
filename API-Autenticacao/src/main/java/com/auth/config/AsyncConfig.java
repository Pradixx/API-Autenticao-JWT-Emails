package com.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Habilita @Async para que o envio de e-mail ocorra em thread separada,
    // sem bloquear a resposta do endpoint de cadastro.
}
