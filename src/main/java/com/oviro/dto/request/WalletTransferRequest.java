package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTransferRequest {

    @NotBlank(message = "Le numéro de téléphone du destinataire est obligatoire")
    @Size(max = 20)
    private String recipientPhone;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1", message = "Montant minimum : 1 UT")
    @DecimalMax(value = "500", message = "Montant maximum : 500 UT par transaction")
    private BigDecimal amount;

    @Size(max = 255)
    private String note;

    @NotBlank(message = "Le PIN est obligatoire")
    @Size(min = 4, max = 4, message = "Le PIN doit comporter 4 chiffres")
    @Pattern(regexp = "\\d{4}", message = "Le PIN doit contenir uniquement des chiffres")
    private String pin;
}
