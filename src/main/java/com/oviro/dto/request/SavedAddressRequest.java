package com.oviro.dto.request;

import com.oviro.enums.AddressType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SavedAddressRequest {

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 100)
    private String label;

    @Size(max = 255)
    private String addressName;

    @NotNull
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    private AddressType type = AddressType.OTHER;

    private boolean isDefault = false;
}
