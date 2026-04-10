package com.oviro.service;

import com.oviro.dto.request.DriverLocationRequest;
import com.oviro.dto.request.SosAlertRequest;
import com.oviro.enums.DriverStatus;
import com.oviro.enums.RideStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.Ride;
import com.oviro.model.SosAlert;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.RideRepository;
import com.oviro.repository.SosAlertRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final RideRepository rideRepository;
    private final SosAlertRepository sosAlertRepository;
    private final SecurityContextHelper securityHelper;

    @Transactional
    public void setOnlineStatus(boolean online) {
        UUID userId = securityHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", userId.toString()));

        if (online && driver.getStatus() == DriverStatus.ON_RIDE) {
            throw new BusinessException("Impossible de passer hors-ligne pendant une course active");
        }

        driver.setStatus(online ? DriverStatus.ONLINE : DriverStatus.OFFLINE);
        driverProfileRepository.save(driver);
        log.info("Chauffeur {} -> {}", userId, driver.getStatus());
    }

    @Transactional
    public void updateLocation(DriverLocationRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", userId.toString()));
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
        UUID userId = securityHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", userId.toString()));

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
        log.warn("ALERTE SOS déclenchée par chauffeur {} à ({}, {})",
                userId, request.getLatitude(), request.getLongitude());
        return alert;
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getNearbyDrivers(BigDecimal lat, BigDecimal lng, double radiusKm) {
        return driverProfileRepository.findNearbyOnlineDrivers(lat, lng, radiusKm);
    }

    @Transactional(readOnly = true)
    public DriverProfile getMyProfile() {
        UUID userId = securityHelper.getCurrentUserId();
        return driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", userId.toString()));
    }
}
