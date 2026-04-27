package com.oviro.model;

import com.oviro.enums.SupportTicketCategory;
import com.oviro.enums.SupportTicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_ticket_user", columnList = "user_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_reference", columnList = "reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket extends BaseEntity {

    @Column(name = "reference", unique = true, nullable = false, length = 20)
    private String reference;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private SupportTicketCategory category;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(name = "related_ride_id")
    private UUID relatedRideId;

    @Column(name = "related_delivery_id")
    private UUID relatedDeliveryId;

    @Column(name = "related_order_id")
    private UUID relatedOrderId;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<SupportMessage> messages = new ArrayList<>();
}
