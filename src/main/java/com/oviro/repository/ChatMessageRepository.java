package com.oviro.repository;

import com.oviro.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByRideIdOrderBySentAtAsc(UUID rideId);

    List<ChatMessage> findByRideIdAndRecipientIdAndReadFalse(UUID rideId, UUID recipientId);

    long countByRideIdAndRecipientIdAndReadFalse(UUID rideId, UUID recipientId);

    @Modifying
    @Query("""
            UPDATE ChatMessage m
               SET m.read = true, m.readAt = :readAt
             WHERE m.ride.id = :rideId AND m.recipient.id = :recipientId AND m.read = false
            """)
    int markAsReadByRideAndRecipient(
            @Param("rideId") UUID rideId,
            @Param("recipientId") UUID recipientId,
            @Param("readAt") LocalDateTime readAt);
}
