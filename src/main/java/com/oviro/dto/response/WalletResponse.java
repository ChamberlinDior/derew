package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder
public class WalletResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private String currency;
    private boolean active;
}
