package com.oviro.dto.response;

import com.oviro.enums.QrCodeStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class QrCodeResponse {
    private UUID id;
    private UUID rideId;
    private String token;
    private String qrCodeImage; // Base64
    private BigDecimal amount;
    private QrCodeStatus status;
    private LocalDateTime expiresAt;
    private boolean valid;
}
