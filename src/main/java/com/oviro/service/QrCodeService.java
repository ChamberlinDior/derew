package com.oviro.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.oviro.dto.request.QrValidationRequest;
import com.oviro.dto.response.QrCodeResponse;
import com.oviro.enums.QrCodeStatus;
import com.oviro.enums.RideStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.*;
import com.oviro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    private final QrCodePaymentRepository qrCodePaymentRepository;
    private final RideRepository rideRepository;
    private final WalletService walletService;

    @Value("${oviro.qr-code.validity-seconds:300}")
    private int validitySeconds;

    @Value("${oviro.qr-code.size:300}")
    private int qrSize;

    @Value("${oviro.commission.default-rate:0.15}")
    private double commissionRate;

    /**
     * Génère un QR code sécurisé et unique pour le paiement d'une course.
     */
    @Transactional
    public QrCodeResponse generateQrCode(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new BusinessException("Le QR code n'est générable que pour une course terminée", "RIDE_NOT_COMPLETED");
        }
        if (qrCodePaymentRepository.existsByRideIdAndStatus(rideId, QrCodeStatus.ACTIVE)) {
            throw new BusinessException("Un QR code actif existe déjà pour cette course", "QR_ALREADY_EXISTS");
        }

        String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(validitySeconds);

        // Payload JSON signé
        String payload = buildPayload(token, rideId, ride.getActualFare(), expiresAt);
        String qrImage = generateQrImage(payload);

        QrCodePayment qr = QrCodePayment.builder()
                .ride(ride)
                .token(token)
                .payload(payload)
                .qrCodeImage(qrImage)
                .amount(ride.getActualFare())
                .expiresAt(expiresAt)
                .build();

        qr = qrCodePaymentRepository.save(qr);
        log.info("QR code généré pour course {}, expire à {}", rideId, expiresAt);
        return mapToResponse(qr);
    }

    /**
     * Valide et traite le paiement via QR code.
     * Anti-duplication : une course ne peut être payée qu'une seule fois.
     */
    @Transactional
    public QrCodeResponse validateQrCode(QrValidationRequest request) {
        QrCodePayment qr = qrCodePaymentRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("QR code introuvable ou invalide", "QR_NOT_FOUND"));

        // Anti-duplication
        if (qr.getStatus() == QrCodeStatus.USED) {
            throw new BusinessException("Ce QR code a déjà été utilisé", "QR_ALREADY_USED");
        }
        if (qr.getStatus() == QrCodeStatus.EXPIRED || LocalDateTime.now().isAfter(qr.getExpiresAt())) {
            qr.setStatus(QrCodeStatus.EXPIRED);
            qrCodePaymentRepository.save(qr);
            throw new BusinessException("Ce QR code a expiré", "QR_EXPIRED");
        }
        if (qr.getStatus() != QrCodeStatus.ACTIVE) {
            throw new BusinessException("QR code invalide", "QR_INVALID");
        }

        // Incrémenter le compteur de scan (traçabilité)
        qr.setScanCount(qr.getScanCount() + 1);
        qr.setScannedAt(LocalDateTime.now());

        Ride ride = qr.getRide();
        UUID clientId = ride.getClient().getId();
        UUID driverUserId = ride.getDriver().getUser().getId();

        // Calcul commission
        java.math.BigDecimal totalAmount = qr.getAmount();
        java.math.BigDecimal commission = totalAmount.multiply(java.math.BigDecimal.valueOf(commissionRate))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal driverEarning = totalAmount.subtract(commission);

        // Débit client
        Transaction clientTx = walletService.debitForRide(clientId, totalAmount, ride.getId());

        // Crédit chauffeur (net commission)
        walletService.creditDriver(driverUserId, driverEarning,
                "Gain course " + ride.getReference());

        // Mise à jour course
        ride.setStatus(RideStatus.PAID);
        ride.setCommissionAmount(commission);
        ride.setTransaction(clientTx);
        rideRepository.save(ride);

        // Mettre à jour le QR
        qr.setStatus(QrCodeStatus.USED);
        qr.setValidatedAt(LocalDateTime.now());
        qr = qrCodePaymentRepository.save(qr);

        log.info("Paiement QR validé: course={}, montant={} XAF, commission={} XAF",
                ride.getReference(), totalAmount, commission);
        return mapToResponse(qr);
    }

    @Transactional(readOnly = true)
    public QrCodeResponse getQrByRide(UUID rideId) {
        QrCodePayment qr = qrCodePaymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("QR code", rideId.toString()));
        return mapToResponse(qr);
    }

    /** Tâche planifiée : expiration automatique des QR codes toutes les minutes */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOldQrCodes() {
        int count = qrCodePaymentRepository.expireOldQrCodes(LocalDateTime.now());
        if (count > 0) log.debug("{} QR code(s) expirés automatiquement", count);
    }

    private String buildPayload(String token, UUID rideId, java.math.BigDecimal amount, LocalDateTime expiresAt) {
        return String.format(
                "{\"token\":\"%s\",\"rideId\":\"%s\",\"amount\":%s,\"expiresAt\":\"%s\",\"issuer\":\"OVIRO\"}",
                token, rideId, amount, expiresAt
        );
    }

    private String generateQrImage(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            log.error("Erreur génération QR image", e);
            return "";
        }
    }

    private QrCodeResponse mapToResponse(QrCodePayment qr) {
        return QrCodeResponse.builder()
                .id(qr.getId())
                .rideId(qr.getRide().getId())
                .token(qr.getToken())
                .qrCodeImage(qr.getQrCodeImage())
                .amount(qr.getAmount())
                .status(qr.getStatus())
                .expiresAt(qr.getExpiresAt())
                .valid(qr.isValid())
                .build();
    }
}
