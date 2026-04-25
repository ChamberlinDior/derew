package com.oviro.service;

import com.oviro.dto.request.RentalRequest;
import com.oviro.dto.response.RentalResponse;
import com.oviro.enums.RentalStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.User;
import com.oviro.model.VehicleRental;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.UserRepository;
import com.oviro.repository.VehicleRentalRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleRentalService {

    private static final BigDecimal PRICE_PER_HOUR_UT = BigDecimal.valueOf(5);

    private final VehicleRentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SecurityContextHelper securityContextHelper;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @Value("${oviro.wallet.fcfa-per-ut:1000}")
    private BigDecimal fcfaPerUt;

    @Transactional
    public RentalResponse requestRental(RentalRequest request) {
        UUID passengerId = securityContextHelper.getCurrentUserId();
        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", passengerId.toString()));

        boolean hasActive = rentalRepository.findByPassengerId(passengerId, Pageable.unpaged())
                .stream()
                .anyMatch(r -> r.getStatus() == RentalStatus.REQUESTED
                        || r.getStatus() == RentalStatus.ACCEPTED
                        || r.getStatus() == RentalStatus.ACTIVE);
        if (hasActive) {
            throw new BusinessException("Vous avez déjà une location en cours", "RENTAL_ALREADY_ACTIVE");
        }

        BigDecimal totalAmount = PRICE_PER_HOUR_UT.multiply(BigDecimal.valueOf(request.getDurationHours()));

        VehicleRental rental = VehicleRental.builder()
                .passenger(passenger)
                .durationHours(request.getDurationHours())
                .pricePerHour(PRICE_PER_HOUR_UT)
                .totalAmount(totalAmount)
                .pickupAddress(request.getPickupAddress())
                .notes(request.getNotes())
                .build();

        rental = rentalRepository.save(rental);
        log.info("Location demandée id={} passager={}", rental.getId(), passengerId);
        return RentalResponse.from(rental, fcfaPerUt);
    }

    @Transactional
    public RentalResponse acceptRental(UUID rentalId) {
        UUID driverUserId = securityContextHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));

        VehicleRental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", rentalId.toString()));

        if (rental.getStatus() != RentalStatus.REQUESTED) {
            throw new BusinessException("Cette location n'est plus disponible", "RENTAL_NOT_AVAILABLE");
        }

        rental.setDriver(driver);
        if (driver.getCurrentVehicle() != null) {
            rental.setVehicle(driver.getCurrentVehicle());
        }
        rental.setStatus(RentalStatus.ACCEPTED);
        rental = rentalRepository.save(rental);

        log.info("Location {} acceptée par chauffeur {}", rentalId, driverUserId);
        return RentalResponse.from(rental, fcfaPerUt);
    }

    @Transactional
    public RentalResponse startRental(UUID rentalId) {
        UUID driverUserId = securityContextHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));

        VehicleRental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", rentalId.toString()));

        if (rental.getStatus() != RentalStatus.ACCEPTED) {
            throw new BusinessException("La location doit être acceptée avant de démarrer", "RENTAL_NOT_ACCEPTED");
        }
        if (!rental.getDriver().getId().equals(driver.getId())) {
            throw new BusinessException("Vous n'êtes pas le chauffeur de cette location", "NOT_YOUR_RENTAL");
        }

        rental.setStatus(RentalStatus.ACTIVE);
        rental.setStartTime(LocalDateTime.now());
        rental.setEndTime(rental.getStartTime().plusHours(rental.getDurationHours()));
        rental = rentalRepository.save(rental);

        log.info("Location {} démarrée, fin prévue {}", rentalId, rental.getEndTime());
        return RentalResponse.from(rental, fcfaPerUt);
    }

    @Transactional
    public RentalResponse completeRental(UUID rentalId) {
        UUID driverUserId = securityContextHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", driverUserId.toString()));

        VehicleRental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", rentalId.toString()));

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BusinessException("La location n'est pas en cours", "RENTAL_NOT_ACTIVE");
        }
        if (!rental.getDriver().getId().equals(driver.getId())) {
            throw new BusinessException("Vous n'êtes pas le chauffeur de cette location", "NOT_YOUR_RENTAL");
        }

        // Débiter le passager
        walletService.debitForRide(
                rental.getPassenger().getId(),
                rental.getTotalAmount(),
                rental.getId()
        );

        // Créditer le chauffeur (85% après commission 15%)
        BigDecimal driverShare = rental.getTotalAmount()
                .multiply(BigDecimal.valueOf(0.85))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        walletService.creditDriver(
                driverUserId,
                driverShare,
                "Paiement location #" + rental.getId()
        );

        rental.setStatus(RentalStatus.COMPLETED);
        rental.setEndTime(LocalDateTime.now());
        rental = rentalRepository.save(rental);

        log.info("Location {} terminée, passager débité {} UT", rentalId, rental.getTotalAmount());
        notificationService.notifyRideCompleted(null); // uses generic completed notification
        return RentalResponse.from(rental, fcfaPerUt);
    }

    @Transactional
    public RentalResponse cancelRental(UUID rentalId) {
        UUID userId = securityContextHelper.getCurrentUserId();

        VehicleRental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", rentalId.toString()));

        if (rental.getStatus() == RentalStatus.ACTIVE || rental.getStatus() == RentalStatus.COMPLETED) {
            throw new BusinessException("Cette location ne peut plus être annulée", "RENTAL_NOT_CANCELLABLE");
        }

        boolean isPassenger = rental.getPassenger().getId().equals(userId);
        boolean isDriver = rental.getDriver() != null
                && rental.getDriver().getUser().getId().equals(userId);

        if (!isPassenger && !isDriver) {
            throw new BusinessException("Vous ne pouvez pas annuler cette location", "NOT_YOUR_RENTAL");
        }

        rental.setStatus(RentalStatus.CANCELLED);
        rental = rentalRepository.save(rental);
        log.info("Location {} annulée par userId={}", rentalId, userId);
        return RentalResponse.from(rental, fcfaPerUt);
    }

    @Transactional(readOnly = true)
    public Page<RentalResponse> getMyRentals(Pageable pageable) {
        UUID passengerId = securityContextHelper.getCurrentUserId();
        return rentalRepository.findByPassengerId(passengerId, pageable)
                .map(r -> RentalResponse.from(r, fcfaPerUt));
    }

    @Transactional(readOnly = true)
    public Page<RentalResponse> getAvailableRentals(Pageable pageable) {
        return rentalRepository.findByStatus(RentalStatus.REQUESTED, pageable)
                .map(r -> RentalResponse.from(r, fcfaPerUt));
    }

    @Transactional(readOnly = true)
    public RentalResponse getRentalById(UUID rentalId) {
        return rentalRepository.findById(rentalId)
                .map(r -> RentalResponse.from(r, fcfaPerUt))
                .orElseThrow(() -> new ResourceNotFoundException("Location", rentalId.toString()));
    }
}
