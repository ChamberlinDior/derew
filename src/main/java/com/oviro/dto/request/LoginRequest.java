package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "L'identifiant est obligatoire")
    private String identifier; // email ou téléphone

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    private String deviceInfo;
}
