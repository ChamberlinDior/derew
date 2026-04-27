package com.oviro.repository;

import com.oviro.enums.SupportTicketStatus;
import com.oviro.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Page<SupportTicket> findByUserId(UUID userId, Pageable pageable);

    Page<SupportTicket> findByStatus(SupportTicketStatus status, Pageable pageable);

    Optional<SupportTicket> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByReference(String reference);

    long countByStatus(SupportTicketStatus status);
}
