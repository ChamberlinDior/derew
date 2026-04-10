package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "daily_limit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal dailyLimit = BigDecimal.valueOf(500000);

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    /**
     * Crédite le wallet en toute sécurité
     */
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du crédit doit être positif");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Débite le wallet avec vérification du solde
     */
    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du débit doit être positif");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Solde insuffisant");
        }
        this.balance = this.balance.subtract(amount);
    }
}
