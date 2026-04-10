package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QrValidationRequest {
    @NotBlank(message = "Le token QR est obligatoire")
    private String token;
}
