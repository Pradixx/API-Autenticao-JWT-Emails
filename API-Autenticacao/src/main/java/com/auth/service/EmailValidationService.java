package com.auth.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class EmailValidationService {

    // ===== Regex de Formato =====
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    // ===== Domínios Descartáveis (Disposable) =====
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(

            // Mailinator
            "mailinator.com", "mailinator2.com", "mailinator.net",

            // Guerrilla Mail
            "guerrillamail.com", "guerrillamail.info", "guerrillamail.biz",
            "guerrillamail.de", "guerrillamail.net", "guerrillamail.org",
            "grr.la", "sharklasers.com", "guerrillamailblock.com",
            "spam4.me",

            // YopMail
            "yopmail.com", "yopmail.fr", "cool.fr.nf", "jetable.fr.nf",
            "nospam.ze.tc", "nomail.xl.cx", "mega.zik.dj", "speed.1s.fr",
            "courriel.fr.nf", "moncourrier.fr.nf", "monemail.fr.nf",
            "monmail.fr.nf",

            // Temp Mail / 10 Minute Mail
            "tempmail.com", "temp-mail.org", "tempmail.net", "tempmail.de",
            "10minutemail.com", "10minutemail.net", "10minutemail.de",
            "10minutemail.co.za", "10minutemail.org",

            // Trash Mail
            "trashmail.com", "trashmail.me", "trashmail.at", "trashmail.io",
            "trashmail.net", "trashmail.org", "trashmail.xyz",

            // Fake Inbox / Mail Drop
            "fakeinbox.com", "maildrop.cc", "mailnull.com",
            "spamgourmet.com", "spamgourmet.net", "spamgourmet.org",

            // Discard / Drop Mail
            "discard.email", "discardmail.com", "discardmail.de",
            "dropmail.me", "throwaway.email", "throwam.com",

            // Get Air Mail / Filz Mail
            "getairmail.com", "filzmail.com",

            // Domínios de gerador automático (Fake Name Generator)
            "armyspy.com", "cuvox.de", "dayrep.com", "einrot.com",
            "fleckens.hu", "gustr.com", "jourrapide.com", "rhyta.com",
            "superrito.com", "teleworm.us",

            // Outros populares
            "dispostable.com", "tempinbox.com", "owlpic.com",
            "33mail.com", "mailexpire.com", "tempr.email",
            "spamfree24.org", "spamfree24.de", "spamfree24.eu",
            "spamfree24.info", "spamfree24.net",
            "spamspot.com", "spamthis.co.uk", "spamtroll.net",
            "spamhereplease.com", "spamherelots.com", "spamgoes.in",
            "getnada.com", "mailnesia.com",
            "0-mail.com", "0815.ru", "0clickemail.com",
            "spambog.com", "spambox.us", "mytemp.email"

    );

    // ===== Métodos Públicos =====

    /**
     * Verifica se o e-mail possui formato válido e não pertence a um domínio descartável.
     *
     * @param email e-mail a ser validado
     * @throws IllegalArgumentException se o e-mail for inválido ou descartável
     */
    public void validate(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail não pode ser vazio.");
        }

        String normalized = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }

        String domain = extractDomain(normalized);

        if (DISPOSABLE_DOMAINS.contains(domain)) {
            throw new IllegalArgumentException(
                    "E-mails de domínios temporários ou descartáveis não são permitidos."
            );
        }
    }

    /**
     * Verifica se o e-mail é válido sem lançar exceção.
     *
     * @param email e-mail a ser verificado
     * @return true se válido, false caso contrário
     */
    public boolean isValid(String email) {
        try {
            validate(email);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ===== Método Privado =====

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return email.substring(atIndex + 1);
    }

}
