package com.oviro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalRequest {

    @NotNull(message = "Le nombre d'heures est obligatoire")
    @Min(value = 1, message = "La durée minimale est 1 heure")
    @Max(value = 24, message = "La durée maximale est 24 heures")
    private Integer durationHours;

    @NotBlank(message = "L'adresse de prise en charge est obligatoire")
    private String pickupAddress;

    private String notes;
}
