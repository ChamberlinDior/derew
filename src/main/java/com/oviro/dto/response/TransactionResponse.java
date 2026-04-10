package com.oviro.dto.response;

import com.oviro.enums.TransactionStatus;
import com.oviro.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class TransactionResponse {
    private UUID id;
    private String reference;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;
}
