package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PartnerProfileResponse {

    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String companyName;
    private String companyRegistrationNumber;
    private String address;
    private BigDecimal commissionRate;
    private boolean verified;
    private int driverCount;
    private LocalDateTime createdAt;
}
