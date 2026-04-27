package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverKycRequest {

    @NotBlank
    private String licenseNumber;

    private String licenseExpiryDate;

    @NotBlank
    private String nationalId;
}
