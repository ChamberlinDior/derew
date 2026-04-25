package com.oviro.dto.response;

import com.oviro.enums.RentalStatus;
import com.oviro.model.VehicleRental;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RentalResponse {

    private UUID id;
    private UUID passengerId;
    private String passengerName;
    private UUID driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleMake;
    private String vehicleModel;
    private String vehiclePlate;
    private String vehicleColor;
    private int durationHours;
    private BigDecimal pricePerHour;
    private BigDecimal totalAmount;
    private BigDecimal totalAmountFcfa;
    private RentalStatus status;
    private String pickupAddress;
    private String notes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;

    public static RentalResponse from(VehicleRental r, BigDecimal fcfaPerUt) {
        RentalResponseBuilder b = RentalResponse.builder()
                .id(r.getId())
                .passengerId(r.getPassenger().getId())
                .passengerName(r.getPassenger().getFullName())
                .durationHours(r.getDurationHours())
                .pricePerHour(r.getPricePerHour())
                .totalAmount(r.getTotalAmount())
                .status(r.getStatus())
                .pickupAddress(r.getPickupAddress())
                .notes(r.getNotes())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .createdAt(r.getCreatedAt());

        if (r.getTotalAmount() != null) {
            b.totalAmountFcfa(r.getTotalAmount().multiply(fcfaPerUt));
        }

        if (r.getDriver() != null) {
            b.driverId(r.getDriver().getId())
             .driverName(r.getDriver().getUser().getFullName())
             .driverPhone(r.getDriver().getUser().getPhoneNumber());
        }

        if (r.getVehicle() != null) {
            b.vehicleMake(r.getVehicle().getMake())
             .vehicleModel(r.getVehicle().getModel())
             .vehiclePlate(r.getVehicle().getPlateNumber())
             .vehicleColor(r.getVehicle().getColor());
        }

        return b.build();
    }
}
