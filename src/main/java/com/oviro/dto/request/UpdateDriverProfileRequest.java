package com.oviro.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateDriverProfileRequest {

    @Size(max = 255)
    private String profilePictureUrl;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 200)
    private String email;

    @Size(max = 20)
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @Size(max = 50)
    private String licenseNumber;

    private LocalDate licenseExpiryDate;

    @Size(max = 50)
    private String nationalId;

    @Size(max = 100)
    private String vehicleMake;

    @Size(max = 100)
    private String vehicleModel;

    private Integer vehicleYear;

    @Size(max = 50)
    private String vehicleColor;

    @Size(max = 50)
    private String vehicleType;

    private Integer vehicleSeats;

    @Size(max = 50)
    private String plateNumber;
}