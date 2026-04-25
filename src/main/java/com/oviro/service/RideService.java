package com.oviro.service;

import com.oviro.dto.request.RatingRequest;
import com.oviro.dto.request.RideRequest;
import com.oviro.dto.response.RideResponse;
import com.oviro.enums.RideStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.Ride;
import com.oviro.model.User;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.RideRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.FareCalculator;
import com.oviro.util.ReferenceGenerator;
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
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final FareCalculator fareCalculator;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationService notificationService;

    @Transactional
    public RideResponse requestRide(RideRequest request) {
        UUID clientId = securityContextHelper.getCurrentUserId();
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", clientId.toString()));

        List<RideStatus> activeStatuses = List.of(
                RideStatus.REQUESTED,
                RideStatus.DRIVER_ASSIGNED,
                RideStatus.DRIVER_EN_ROUTE,
                RideStatus.DRIVER_ARRIVED,
                RideStatus.IN_PROGRESS
        );

        List<Ride> active = rideRepository.findActiveRidesByClient(clientId, activeStatuses);
        if (!active.isEmpty()) {
            throw new BusinessException("Une course est déjà en cours", "RIDE_ALREADY_ACTIVE");
        }

        BigDecimal distance = fareCalculator.calculateDistance(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropoffLatitude(),
                request.getDropoffLongitude()
        );
        int duration = fareCalculator.estimateDuration(distance);
        BigDecimal fare = fareCalculator.calculateFare(distance, duration);

        Ride ride = Ride.builder()
                .reference(referenceGenerator.generateRideReference())
                .client(client)
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .dropoffAddress(request.getDropoffAddress())
                .dropoffLatitude(request.getDropoffLatitude())
                .dropoffLongitude(request.getDropoffLongitude())
                .estimatedDistanceKm(distance)
                .estimatedDurationMinutes(duration)
                .estimatedFare(fare)
                .requestedAt(LocalDateTime.now())
                .build();

        ride = rideRepository.save(ride);
        log.info("Course demandée: {} par client {}", ride.getReference(), clientId);

        return mapToResponse(ride);
    }

    @Transactional
    public RideResponse acceptRide(UUID rideId) {
        UUID driverUserId = securityContextHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new BusinessException("Cette course n'est plus disponible", "RIDE_NOT_AVAILABLE");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);
        ride.setAssignedAt(LocalDateTime.now());
        ride = rideRepository.save(ride);

        log.info("Course {} acceptée par chauffeur {}", ride.getReference(), driverUserId);
        notificationService.notifyRideAccepted(ride);
        return mapToResponse(ride);
    }

    @Transactional
    public RideResponse updateStatus(UUID rideId, RideStatus newStatus) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        validateStatusTransition(ride.getStatus(), newStatus);

        ride.setStatus(newStatus);

        switch (newStatus) {
            case DRIVER_EN_ROUTE -> { }
            case DRIVER_ARRIVED  -> { }
            case IN_PROGRESS     -> ride.setStartedAt(LocalDateTime.now());
            case COMPLETED -> {
                ride.setCompletedAt(LocalDateTime.now());
                ride.setActualFare(ride.getEstimatedFare());
            }
            default -> { }
        }

        ride = rideRepository.save(ride);

        // Fire notifications based on the new status
        switch (newStatus) {
            case DRIVER_ARRIVED -> notificationService.notifyDriverArrived(ride);
            case IN_PROGRESS    -> notificationService.notifyRideStarted(ride);
            case COMPLETED      -> notificationService.notifyRideCompleted(ride);
            default -> { }
        }

        return mapToResponse(ride);
    }

    @Transactional
    public RideResponse cancelRide(UUID rideId, String reason) {
        UUID userId = securityContextHelper.getCurrentUserId();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        List<RideStatus> cancellableStatuses = List.of(
                RideStatus.REQUESTED,
                RideStatus.DRIVER_ASSIGNED,
                RideStatus.DRIVER_EN_ROUTE
        );

        if (!cancellableStatuses.contains(ride.getStatus())) {
            throw new BusinessException("Cette course ne peut plus être annulée", "RIDE_NOT_CANCELLABLE");
        }

        boolean isClient = ride.getClient().getId().equals(userId);

        ride.setStatus(isClient ? RideStatus.CANCELLED_BY_CLIENT : RideStatus.CANCELLED_BY_DRIVER);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancellationReason(reason);

        ride = rideRepository.save(ride);
        return mapToResponse(ride);
    }

    @Transactional
    public RideResponse rateRide(UUID rideId, RatingRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        if (ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.PAID) {
            throw new BusinessException("Seules les courses terminées peuvent être notées");
        }

        if (ride.getClient().getId().equals(userId)) {
            ride.setClientRating(request.getRating());
            ride.setClientReview(request.getReview());
        } else if (ride.getDriver() != null && ride.getDriver().getUser().getId().equals(userId)) {
            ride.setDriverRating(request.getRating());
        } else {
            throw new BusinessException("Vous ne pouvez pas noter cette course");
        }

        ride = rideRepository.save(ride);
        return mapToResponse(ride);
    }

    @Transactional(readOnly = true)
    public RideResponse getRideById(UUID rideId) {
        return rideRepository.findById(rideId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> getMyRides(Pageable pageable) {
        UUID userId = securityContextHelper.getCurrentUserId();
        return rideRepository.findByClientId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> getDriverRides(Pageable pageable) {
        UUID driverUserId = securityContextHelper.getCurrentUserId();

        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));

        return rideRepository.findByDriverId(driver.getId(), pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> getAvailableRides(Double lat, Double lng, Pageable pageable) {
        Page<Ride> rides = rideRepository.findByStatus(RideStatus.REQUESTED, pageable);
        return rides.map(this::mapToResponse);
    }

    private void validateStatusTransition(RideStatus current, RideStatus next) {
        boolean valid = switch (current) {
            case REQUESTED -> next == RideStatus.DRIVER_ASSIGNED || next == RideStatus.CANCELLED_BY_CLIENT;
            case DRIVER_ASSIGNED -> next == RideStatus.DRIVER_EN_ROUTE || next == RideStatus.CANCELLED_BY_DRIVER;
            case DRIVER_EN_ROUTE -> next == RideStatus.DRIVER_ARRIVED;
            case DRIVER_ARRIVED -> next == RideStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == RideStatus.COMPLETED;
            case COMPLETED -> next == RideStatus.PAID;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(
                    "Transition de statut invalide: " + current + " -> " + next,
                    "INVALID_STATUS_TRANSITION"
            );
        }
    }

    private RideResponse mapToResponse(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .reference(ride.getReference())
                .clientId(ride.getClient().getId())
                .clientName(ride.getClient().getFullName())
                .driverId(ride.getDriver() != null ? ride.getDriver().getId() : null)
                .driverName(ride.getDriver() != null ? ride.getDriver().getUser().getFullName() : null)
                .driverPhone(ride.getDriver() != null ? ride.getDriver().getUser().getPhoneNumber() : null)
                .pickupAddress(ride.getPickupAddress())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .dropoffAddress(ride.getDropoffAddress())
                .dropoffLatitude(ride.getDropoffLatitude())
                .dropoffLongitude(ride.getDropoffLongitude())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                .estimatedFare(ride.getEstimatedFare())
                .actualFare(ride.getActualFare())
                .status(ride.getStatus())
                .requestedAt(ride.getRequestedAt())
                .completedAt(ride.getCompletedAt())
                .clientRating(ride.getClientRating())
                .driverRating(ride.getDriverRating())
                .build();
    }
}