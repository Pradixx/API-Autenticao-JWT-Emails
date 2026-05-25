package com.auth.repository;

import com.auth.entity.EmailVerificationToken;
import com.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUser(User user);

    @Transactional
    @Modifying
    void deleteByUser(User user);

    // Remove todos os tokens com expiresAt anterior ao momento informado
    // Usa JPQL direto para evitar carregamento desnecessário das entidades em memória
    @Transactional
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    int deleteAllExpiredBefore(@Param("now") LocalDateTime now);

}
