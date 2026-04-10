package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletRechargeRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "100.0", message = "Montant minimum : 100 XAF")
    @DecimalMax(value = "1000000.0", message = "Montant maximum : 1 000 000 XAF")
    private BigDecimal amount;

    @NotBlank(message = "Le fournisseur Mobile Money est obligatoire")
    private String provider; // MTN_MOMO, AIRTEL_MONEY, etc.

    @NotBlank(message = "Le numéro Mobile Money est obligatoire")
    private String mobileMoneyNumber;

    private String externalReference;
}
