package com.example.backend.Repository;

import com.example.backend.Persistence.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    @Modifying
    @Query("""
            update PasswordResetTokenEntity token
            set token.usedAt = :usedAt
            where token.utilisateurId = :utilisateurId
              and token.usedAt is null
            """)
    int invalidateUnusedTokens(@Param("utilisateurId") Integer utilisateurId,
                               @Param("usedAt") Instant usedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
}
