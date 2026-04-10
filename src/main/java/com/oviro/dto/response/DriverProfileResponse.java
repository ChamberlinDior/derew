package com.oviro.dto.response;

import com.oviro.enums.DriverStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DriverProfileResponse {
    private UUID id;

    private UserSummary user;

    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private String nationalId;

    private DriverStatus status;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;

    private BigDecimal rating;
    private Integer totalRides;
    private BigDecimal totalEarnings;
    private boolean verified;

    private VehicleSummary currentVehicle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class UserSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profilePictureUrl;
        private LocalDateTime dateOfBirth;
    }

    @Data
    @Builder
    public static class VehicleSummary {
        private UUID id;
        private String plateNumber;
        private String make;
        private String model;
        private Integer year;
        private String color;
        private String type;
        private Integer seats;
        private String status;
    }
}