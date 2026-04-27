package com.oviro.service;

import com.oviro.dto.request.DeliveryRequest;
import com.oviro.dto.request.RatingRequest;
import com.oviro.dto.response.DeliveryResponse;
import com.oviro.enums.DeliveryStatus;
import com.oviro.enums.NotificationType;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.Delivery;
import com.oviro.model.DriverProfile;
import com.oviro.model.User;
import com.oviro.repository.DeliveryRepository;
import com.oviro.repository.DriverProfileRepository;
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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final FareCalculator fareCalculator;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityHelper;
    private final NotificationService notificationService;

    @Transactional
    public DeliveryResponse createDelivery(DeliveryRequest request) {
        UUID senderId = securityHelper.getCurrentUserId();
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", senderId.toString()));

        List<DeliveryStatus> activeStatuses = List.of(DeliveryStatus.REQUESTED, DeliveryStatus.DRIVER_ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT);
        if (!deliveryRepository.findActiveBySender(senderId, activeStatuses).isEmpty()) {
            throw new BusinessException("Une livraison est déjà en cours", "DELIVERY_ALREADY_ACTIVE");
        }

        BigDecimal distance = fareCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude());

        // Tarif livraison = tarif MOTO (x0.7) + forfait fixe 500 FCFA
        BigDecimal fare = fareCalculator.calculateFare(distance, fareCalculator.estimateDuration(distance),
                com.oviro.enums.ServiceType.MOTO).add(BigDecimal.valueOf(500));

        String pin = generateDeliveryPin();

        Delivery delivery = Delivery.builder()
                .reference(referenceGenerator.generateRideReference())
                .sender(sender)
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .dropoffAddress(request.getDropoffAddress())
                .dropoffLatitude(request.getDropoffLatitude())
                .dropoffLongitude(request.getDropoffLongitude())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .packageType(request.getPackageType())
                .packageDescription(request.getPackageDescription())
                .packageWeightKg(request.getPackageWeightKg())
                .fragile(request.isFragile())
                .senderNote(request.getSenderNote())
                .paymentMethod(request.getPaymentMethod())
                .estimatedDistanceKm(distance)
                .estimatedFare(fare)
                .deliveryPin(pin)
                .requestedAt(LocalDateTime.now())
                .build();

        delivery = deliveryRepository.save(delivery);
        log.info("Livraison créée: {} par {}", delivery.getReference(), senderId);
        return mapToResponse(delivery);
    }

    @Transactional
    public DeliveryResponse acceptDelivery(UUID deliveryId) {
        UUID driverUserId = securityHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));

        Delivery delivery = findById(deliveryId);
        if (delivery.getStatus() != DeliveryStatus.REQUESTED) {
            throw new BusinessException("Cette livraison n'est plus disponible", "DELIVERY_NOT_AVAILABLE");
        }

        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.DRIVER_ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        delivery = deliveryRepository.save(delivery);

        notificationService.sendToUser(delivery.getSender().getId(), NotificationType.DELIVERY_ASSIGNED,
                "Livreur trouvé", driver.getUser().getFullName() + " prend en charge votre livraison.",
                Map.of("deliveryId", deliveryId.toString(), "type", "DELIVERY_ASSIGNED"), 30L, true);

        return mapToResponse(delivery);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID deliveryId, DeliveryStatus newStatus) {
        Delivery delivery = findById(deliveryId);

        delivery.setStatus(newStatus);
        switch (newStatus) {
            case PICKED_UP -> delivery.setPickedUpAt(LocalDateTime.now());
            case DELIVERED -> delivery.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> delivery.setCancelledAt(LocalDateTime.now());
            default -> {}
        }

        delivery = deliveryRepository.save(delivery);

        if (newStatus == DeliveryStatus.DELIVERED) {
            notificationService.sendToUser(delivery.getSender().getId(), NotificationType.DELIVERY_COMPLETED,
                    "Livraison effectuée",
                    "Votre colis a été remis à " + delivery.getRecipientName() + ".",
                    Map.of("deliveryId", deliveryId.toString()), 60L, false);
        }

        return mapToResponse(delivery);
    }

    @Transactional
    public DeliveryResponse rateDelivery(UUID deliveryId, RatingRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        Delivery delivery = findById(deliveryId);

        if (!delivery.getSender().getId().equals(userId)) {
            throw new BusinessException("Vous ne pouvez pas noter cette livraison");
        }
        if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
            throw new BusinessException("Seules les livraisons terminées peuvent être notées");
        }

        delivery.setSenderRating(request.getRating());
        delivery.setSenderReview(request.getReview());
        return mapToResponse(deliveryRepository.save(delivery));
    }

    @Transactional
    public DeliveryResponse cancel(UUID deliveryId, String reason) {
        Delivery delivery = findById(deliveryId);

        if (delivery.getStatus() == DeliveryStatus.DELIVERED || delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new BusinessException("Cette livraison ne peut plus être annulée");
        }

        delivery.setStatus(DeliveryStatus.CANCELLED);
        delivery.setCancelledAt(LocalDateTime.now());
        delivery.setCancellationReason(reason);
        return mapToResponse(deliveryRepository.save(delivery));
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse> getMySentDeliveries(Pageable pageable) {
        UUID userId = securityHelper.getCurrentUserId();
        return deliveryRepository.findBySenderId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse> getMyDriverDeliveries(Pageable pageable) {
        UUID driverUserId = securityHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));
        return deliveryRepository.findByDriverId(driver.getId(), pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse> getAvailableDeliveries(Pageable pageable) {
        return deliveryRepository.findByStatus(DeliveryStatus.REQUESTED, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(UUID deliveryId) {
        return mapToResponse(findById(deliveryId));
    }

    private Delivery findById(UUID id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison", id.toString()));
    }

    private String generateDeliveryPin() {
        return String.valueOf(1000 + new SecureRandom().nextInt(9000));
    }

    private DeliveryResponse mapToResponse(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .reference(d.getReference())
                .senderId(d.getSender().getId())
                .senderName(d.getSender().getFullName())
                .driverId(d.getDriver() != null ? d.getDriver().getId() : null)
                .driverName(d.getDriver() != null ? d.getDriver().getUser().getFullName() : null)
                .driverPhone(d.getDriver() != null ? d.getDriver().getUser().getPhoneNumber() : null)
                .pickupAddress(d.getPickupAddress())
                .pickupLatitude(d.getPickupLatitude())
                .pickupLongitude(d.getPickupLongitude())
                .dropoffAddress(d.getDropoffAddress())
                .dropoffLatitude(d.getDropoffLatitude())
                .dropoffLongitude(d.getDropoffLongitude())
                .recipientName(d.getRecipientName())
                .recipientPhone(d.getRecipientPhone())
                .packageType(d.getPackageType())
                .packageDescription(d.getPackageDescription())
                .packageWeightKg(d.getPackageWeightKg())
                .fragile(d.isFragile())
                .estimatedDistanceKm(d.getEstimatedDistanceKm())
                .estimatedFare(d.getEstimatedFare())
                .actualFare(d.getActualFare())
                .paymentMethod(d.getPaymentMethod())
                .status(d.getStatus())
                .senderNote(d.getSenderNote())
                .deliveryPin(d.getDeliveryPin())
                .requestedAt(d.getRequestedAt())
                .assignedAt(d.getAssignedAt())
                .pickedUpAt(d.getPickedUpAt())
                .deliveredAt(d.getDeliveredAt())
                .cancelledAt(d.getCancelledAt())
                .cancellationReason(d.getCancellationReason())
                .senderRating(d.getSenderRating())
                .build();
    }
}
