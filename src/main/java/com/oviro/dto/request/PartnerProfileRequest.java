package com.oviro.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PartnerProfileRequest {

    @Size(min = 2, max = 200)
    private String companyName;

    @Size(max = 100)
    private String companyRegistrationNumber;

    @Size(max = 500)
    private String address;
}
