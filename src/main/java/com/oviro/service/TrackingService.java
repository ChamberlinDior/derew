package com.oviro.service;

import com.oviro.dto.request.DriverLocationRequest;
import com.oviro.dto.response.NearbyDriverResponse;
import com.oviro.enums.DriverStatus;
import com.oviro.model.DriverProfile;
import com.oviro.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final DriverProfileRepository driverProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void broadcastDriverLocation(UUID driverUserId, DriverLocationRequest request) {
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId).orElse(null);
        if (driver == null) return;

        Map<String, Object> payload = Map.of(
                "driverId", driver.getId().toString(),
                "latitude", request.getLatitude(),
                "longitude", request.getLongitude(),
                "timestamp", System.currentTimeMillis()
        );

        // Broadcast position au client de la course en cours
        if (driver.getRides() != null) {
            driver.getRides().stream()
                    .filter(r -> r.getStatus().name().startsWith("DRIVER") || "IN_PROGRESS".equals(r.getStatus().name()))
                    .findFirst()
                    .ifPresent(ride -> {
                        messagingTemplate.convertAndSendToUser(
                                ride.getClient().getId().toString(),
                                "/queue/tracking",
                                payload
                        );
                        messagingTemplate.convertAndSend("/topic/ride/" + ride.getId() + "/tracking", payload);
                        log.debug("Position broadcast ride={} driver={}", ride.getId(), driverUserId);
                    });
        }
    }

    @Transactional(readOnly = true)
    public List<NearbyDriverResponse> getNearbyDrivers(BigDecimal lat, BigDecimal lng, double radiusKm) {
        List<DriverProfile> drivers = driverProfileRepository.findNearbyOnlineDrivers(lat, lng, radiusKm);
        return drivers.stream().map(d -> {
            double dist = haversine(lat.doubleValue(), lng.doubleValue(),
                    d.getCurrentLatitude().doubleValue(), d.getCurrentLongitude().doubleValue());
            int eta = (int) Math.ceil(dist / 30.0 * 60);
            return NearbyDriverResponse.builder()
                    .driverId(d.getId())
                    .driverName(d.getUser().getFullName())
                    .latitude(d.getCurrentLatitude())
                    .longitude(d.getCurrentLongitude())
                    .rating(d.getRating())
                    .vehicleType(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getType() : null)
                    .vehicleMake(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getMake() : null)
                    .vehicleModel(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getModel() : null)
                    .vehicleColor(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getColor() : null)
                    .plateNumber(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getPlateNumber() : null)
                    .distanceKm(Math.round(dist * 100.0) / 100.0)
                    .estimatedMinutes(eta)
                    .build();
        }).toList();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
