package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SetPinRequest {

    @NotBlank
    @Size(min = 4, max = 4)
    @Pattern(regexp = "\\d{4}", message = "Le PIN doit contenir 4 chiffres")
    private String pin;
}
