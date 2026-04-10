package com.oviro.model;

import com.oviro.enums.TransactionStatus;
import com.oviro.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_wallet", columnList = "wallet_id"),
    @Index(name = "idx_tx_reference", columnList = "reference", unique = true),
    @Index(name = "idx_tx_type", columnList = "type"),
    @Index(name = "idx_tx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Column(name = "reference", unique = true, nullable = false, length = 30)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "payment_provider", length = 100)
    private String paymentProvider;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
