package com.oviro.model;

import com.oviro.enums.QrCodeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_code_payments", indexes = {
    @Index(name = "idx_qr_token", columnList = "token", unique = true),
    @Index(name = "idx_qr_status", columnList = "status"),
    @Index(name = "idx_qr_ride", columnList = "ride_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCodePayment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    /**
     * Token cryptographiquement sécurisé (UUID v4 + signature)
     */
    @Column(name = "token", unique = true, nullable = false, length = 256)
    private String token;

    /**
     * Contenu encodé dans le QR code (JSON signé)
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * Image QR code encodée en Base64
     */
    @Column(name = "qr_code_image", columnDefinition = "TEXT")
    private String qrCodeImage;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private QrCodeStatus status = QrCodeStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "scan_count")
    @Builder.Default
    private int scanCount = 0;

    /**
     * Vérifie si le QR code est encore valide
     */
    public boolean isValid() {
        return status == QrCodeStatus.ACTIVE && LocalDateTime.now().isBefore(expiresAt);
    }
}
