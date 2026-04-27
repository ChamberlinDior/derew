package com.oviro.service;

import com.oviro.dto.request.ClientSosRequest;
import com.oviro.dto.request.RatingRequest;
import com.oviro.dto.request.RideRequest;
import com.oviro.dto.response.RideEstimateResponse;
import com.oviro.dto.response.RideResponse;
import com.oviro.enums.NotificationType;
import com.oviro.enums.RideStatus;
import com.oviro.enums.ServiceType;
import com.oviro.enums.TransactionType;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.Ride;
import com.oviro.model.RideStop;
import com.oviro.model.User;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.RideRepository;
import com.oviro.repository.RideStopRepository;
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
    private final RideStopRepository rideStopRepository;
    private final FareCalculator fareCalculator;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final ReferralService referralService;

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
        ServiceType serviceType = request.getServiceType() != null ? request.getServiceType() : ServiceType.STANDARD;
        BigDecimal fare = fareCalculator.calculateFare(distance, duration, serviceType);

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
                .serviceType(serviceType)
                .forSomeoneElse(request.isForSomeoneElse())
                .recipientPhone(request.getRecipientPhone())
                .recipientName(request.getRecipientName())
                .senderNote(request.getSenderNote())
                .requestedAt(LocalDateTime.now())
                .build();

        ride = rideRepository.save(ride);

        // Persister les arrêts intermédiaires
        if (request.getStops() != null && !request.getStops().isEmpty()) {
            int order = 1;
            for (RideRequest.StopRequest stopReq : request.getStops()) {
                RideStop stop = RideStop.builder()
                        .ride(ride)
                        .stopOrder(order++)
                        .address(stopReq.getAddress())
                        .latitude(stopReq.getLatitude())
                        .longitude(stopReq.getLongitude())
                        .note(stopReq.getNote())
                        .build();
                rideStopRepository.save(stop);
            }
        }

        // Générer un token de partage
        ride.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
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
                referralService.processReferralOnFirstRide(ride.getClient().getId());
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
    public RideEstimateResponse estimateRide(BigDecimal fromLat, BigDecimal fromLng,
                                             BigDecimal toLat, BigDecimal toLng,
                                             ServiceType serviceType) {
        if (serviceType == null) serviceType = ServiceType.STANDARD;

        BigDecimal distance = fareCalculator.calculateDistance(fromLat, fromLng, toLat, toLng);
        int duration = fareCalculator.estimateDuration(distance);
        BigDecimal fare = fareCalculator.calculateFare(distance, duration, serviceType);

        BigDecimal base = BigDecimal.valueOf(500);
        BigDecimal distanceCost = distance.multiply(BigDecimal.valueOf(200));
        BigDecimal timeCost = BigDecimal.valueOf(duration).multiply(BigDecimal.valueOf(20));

        return RideEstimateResponse.builder()
                .serviceType(serviceType)
                .estimatedPrice(fare)
                .estimatedDurationMinutes(duration)
                .estimatedDistanceKm(distance)
                .priceBreakdown(java.util.Map.of(
                        "base", base,
                        "distance", distanceCost,
                        "time", timeCost
                ))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> getAvailableRides(Double lat, Double lng, Pageable pageable) {
        Page<Ride> rides = rideRepository.findByStatus(RideStatus.REQUESTED, pageable);
        return rides.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public RideResponse getRideByShareToken(String shareToken) {
        return rideRepository.findByShareToken(shareToken)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Course", shareToken));
    }

    @Transactional
    public void triggerClientSos(UUID rideId, ClientSosRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        java.util.Map<String, String> data = java.util.Map.of(
                "type", NotificationType.CLIENT_SOS_ALERT.name(),
                "rideId", rideId.toString(),
                "clientId", userId.toString(),
                "latitude", request.getLatitude().toPlainString(),
                "longitude", request.getLongitude().toPlainString()
        );

        List<com.oviro.model.User> admins = userRepository.findAllByRole(com.oviro.enums.Role.ADMIN);
        for (com.oviro.model.User admin : admins) {
            notificationService.sendToUser(admin.getId(), NotificationType.CLIENT_SOS_ALERT,
                    "SOS Passager", "Un passager a déclenché une alerte SOS sur la course " + ride.getReference(),
                    data, 30L, true);
        }

        if (ride.getDriver() != null) {
            notificationService.sendToUser(ride.getDriver().getUser().getId(), NotificationType.CLIENT_SOS_ALERT,
                    "SOS Passager", "Votre passager a signalé une urgence.", data, 30L, true);
        }

        log.info("SOS client déclenché sur course {} par userId={}", rideId, userId);
    }

    @Transactional
    public void addTip(UUID rideId, BigDecimal amount) {
        UUID clientId = securityContextHelper.getCurrentUserId();
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));

        if (!ride.getClient().getId().equals(clientId)) {
            throw new com.oviro.exception.BusinessException("Vous ne pouvez pas donner un pourboire pour cette course");
        }
        if (ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.PAID) {
            throw new com.oviro.exception.BusinessException("Seules les courses terminées acceptent les pourboires");
        }

        walletService.debitForRide(clientId, amount, rideId);
        if (ride.getDriver() != null) {
            walletService.creditDriver(ride.getDriver().getUser().getId(), amount,
                    "Pourboire course " + ride.getReference());
            notificationService.sendToUser(ride.getDriver().getUser().getId(),
                    NotificationType.TIP_RECEIVED, "Pourboire reçu !",
                    "Vous avez reçu un pourboire de " + amount + " FCFA pour la course " + ride.getReference(),
                    java.util.Map.of("rideId", rideId.toString(), "amount", amount.toPlainString()), 3600L, false);
        }

        ride.setTipAmount(ride.getTipAmount() != null ? ride.getTipAmount().add(amount) : amount);
        rideRepository.save(ride);
        log.info("Pourboire {} FCFA ajouté à la course {} par client {}", amount, rideId, clientId);
    }

    @Transactional(readOnly = true)
    public java.util.Map<ServiceType, RideEstimateResponse> estimateAllServiceTypes(
            BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng) {
        java.util.Map<ServiceType, RideEstimateResponse> result = new java.util.LinkedHashMap<>();
        BigDecimal distance = fareCalculator.calculateDistance(fromLat, fromLng, toLat, toLng);
        int duration = fareCalculator.estimateDuration(distance);
        for (ServiceType st : ServiceType.values()) {
            BigDecimal fare = fareCalculator.calculateFare(distance, duration, st);
            result.put(st, RideEstimateResponse.builder()
                    .serviceType(st)
                    .estimatedPrice(fare)
                    .estimatedDurationMinutes(duration)
                    .estimatedDistanceKm(distance)
                    .priceBreakdown(java.util.Map.of("base", BigDecimal.valueOf(500),
                            "multiplier", BigDecimal.valueOf(st.getPriceMultiplier())))
                    .build());
        }
        return result;
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
                .serviceType(ride.getServiceType())
                .forSomeoneElse(ride.isForSomeoneElse())
                .recipientName(ride.getRecipientName())
                .requestedAt(ride.getRequestedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .clientRating(ride.getClientRating())
                .driverRating(ride.getDriverRating())
                .clientReview(ride.getClientReview())
                .build();
    }
}