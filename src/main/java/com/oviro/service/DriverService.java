package com.oviro.service;

import com.oviro.dto.request.DriverLocationRequest;
import com.oviro.dto.request.SosAlertRequest;
import com.oviro.dto.response.DriverProfileResponse;
import com.oviro.enums.DriverStatus;
import com.oviro.enums.Role;
import com.oviro.enums.RideStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.Ride;
import com.oviro.model.SosAlert;
import com.oviro.model.User;
import com.oviro.model.Vehicle;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.RideRepository;
import com.oviro.repository.SosAlertRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final RideRepository rideRepository;
    private final SosAlertRepository sosAlertRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityHelper;

    @Transactional
    public void setOnlineStatus(boolean online) {
        DriverProfile driver = getOrCreateMyProfileEntity();

        if (!online && driver.getStatus() == DriverStatus.ON_RIDE) {
            throw new BusinessException("Impossible de passer hors-ligne pendant une course active");
        }

        driver.setStatus(online ? DriverStatus.ONLINE : DriverStatus.OFFLINE);
        driverProfileRepository.save(driver);
        log.info("Chauffeur {} -> {}", driver.getUser().getId(), driver.getStatus());
    }

    @Transactional
    public void updateLocation(DriverLocationRequest request) {
        DriverProfile driver = getOrCreateMyProfileEntity();
        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driverProfileRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public Page<Ride> getAvailableRides(BigDecimal lat, BigDecimal lng, Pageable pageable) {
        return rideRepository.findByStatus(RideStatus.REQUESTED, pageable);
    }

    @Transactional
    public SosAlert triggerSos(SosAlertRequest request) {
        DriverProfile driver = getOrCreateMyProfileEntity();

        driver.setStatus(DriverStatus.SOS);
        driverProfileRepository.save(driver);

        Ride ride = null;
        if (request.getRideId() != null) {
            ride = rideRepository.findById(request.getRideId()).orElse(null);
        }

        SosAlert alert = SosAlert.builder()
                .driver(driver)
                .ride(ride)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .build();

        alert = sosAlertRepository.save(alert);
        log.warn(
                "ALERTE SOS déclenchée par chauffeur {} à ({}, {})",
                driver.getUser().getId(),
                request.getLatitude(),
                request.getLongitude()
        );
        return alert;
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getNearbyDrivers(BigDecimal lat, BigDecimal lng, double radiusKm) {
        return driverProfileRepository.findNearbyOnlineDrivers(lat, lng, radiusKm);
    }

    @Transactional
    public DriverProfileResponse getMyProfile() {
        DriverProfile profile = getOrCreateMyProfileEntity();
        return mapToResponse(profile);
    }

    @Transactional
    public DriverProfile getOrCreateMyProfileEntity() {
        UUID userId = securityHelper.getCurrentUserId();

        return driverProfileRepository.findByUserId(userId)
                .orElseGet(() -> createMissingDriverProfile(userId));
    }

    private DriverProfile createMissingDriverProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        if (user.getRole() != Role.DRIVER) {
            throw new BusinessException("L'utilisateur connecté n'a pas le rôle DRIVER");
        }

        DriverProfile profile = DriverProfile.builder()
                .user(user)
                .licenseNumber(generateDriverLicenseNumber(user))
                .status(DriverStatus.OFFLINE)
                .verified(false)
                .build();

        DriverProfile saved = driverProfileRepository.save(profile);
        log.warn("DriverProfile auto-créé pour userId={}", userId);
        return saved;
    }

    private String generateDriverLicenseNumber(User user) {
        String base = "DRV-" + user.getId().toString().substring(0, 8).toUpperCase();
        String candidate = base;
        int suffix = 1;

        while (driverProfileRepository.findByLicenseNumber(candidate).isPresent()) {
            candidate = base + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private DriverProfileResponse mapToResponse(DriverProfile profile) {
        return DriverProfileResponse.builder()
                .id(profile.getId())
                .user(mapUser(profile.getUser()))
                .licenseNumber(profile.getLicenseNumber())
                .licenseExpiryDate(profile.getLicenseExpiryDate())
                .nationalId(profile.getNationalId())
                .status(profile.getStatus())
                .currentLatitude(profile.getCurrentLatitude())
                .currentLongitude(profile.getCurrentLongitude())
                .rating(profile.getRating())
                .totalRides(profile.getTotalRides())
                .totalEarnings(profile.getTotalEarnings())
                .verified(profile.isVerified())
                .currentVehicle(mapVehicle(profile.getCurrentVehicle()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private DriverProfileResponse.UserSummary mapUser(User user) {
        if (user == null) return null;

        return DriverProfileResponse.UserSummary.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    private DriverProfileResponse.VehicleSummary mapVehicle(Vehicle vehicle) {
        if (vehicle == null) return null;

        return DriverProfileResponse.VehicleSummary.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .type(vehicle.getType())
                .seats(vehicle.getSeats())
                .status(vehicle.getStatus() != null ? vehicle.getStatus().name() : null)
                .build();
    }
}