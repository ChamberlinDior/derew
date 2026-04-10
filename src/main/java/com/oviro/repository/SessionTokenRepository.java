package com.oviro.repository;

import com.oviro.model.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionTokenRepository extends JpaRepository<SessionToken, UUID> {

    Optional<SessionToken> findByToken(String token);

    List<SessionToken> findByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("UPDATE SessionToken s SET s.revoked = true, s.revokedAt = CURRENT_TIMESTAMP WHERE s.user.id = :userId AND s.revoked = false")
    int revokeAllUserSessions(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE SessionToken s SET s.revoked = true, s.revokedAt = CURRENT_TIMESTAMP WHERE s.token = :token")
    int revokeByToken(@Param("token") String token);
}
