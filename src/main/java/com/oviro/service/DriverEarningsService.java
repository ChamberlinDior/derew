package com.oviro.service;

import com.oviro.dto.response.DriverEarningsResponse;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverBonusTarget;
import com.oviro.model.DriverProfile;
import com.oviro.repository.DriverBonusTargetRepository;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.RideRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverEarningsService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverBonusTargetRepository bonusTargetRepository;
    private final RideRepository rideRepository;
    private final SecurityContextHelper securityHelper;

    @Transactional(readOnly = true)
    public DriverEarningsResponse getMyEarnings() {
        UUID userId = securityHelper.getCurrentUserId();
        DriverProfile driver = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil chauffeur", userId.toString()));

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        BigDecimal todayEarnings = rideRepository.sumEarningsByDriverAndPeriod(driver.getId(), startOfDay, LocalDateTime.now());
        BigDecimal weekEarnings = rideRepository.sumEarningsByDriverAndPeriod(driver.getId(), startOfWeek, LocalDateTime.now());
        BigDecimal monthEarnings = rideRepository.sumEarningsByDriverAndPeriod(driver.getId(), startOfMonth, LocalDateTime.now());

        long todayRides = rideRepository.countCompletedByDriverAndPeriod(driver.getId(), startOfDay, LocalDateTime.now());
        long weekRides = rideRepository.countCompletedByDriverAndPeriod(driver.getId(), startOfWeek, LocalDateTime.now());
        long monthRides = rideRepository.countCompletedByDriverAndPeriod(driver.getId(), startOfMonth, LocalDateTime.now());

        Optional<DriverBonusTarget> todayBonus = bonusTargetRepository.findByDriverIdAndTargetDate(driver.getId(), today);

        DriverEarningsResponse.DriverEarningsResponseBuilder builder = DriverEarningsResponse.builder()
                .todayEarnings(todayEarnings != null ? todayEarnings : BigDecimal.ZERO)
                .weekEarnings(weekEarnings != null ? weekEarnings : BigDecimal.ZERO)
                .monthEarnings(monthEarnings != null ? monthEarnings : BigDecimal.ZERO)
                .totalEarnings(driver.getTotalEarnings())
                .todayRides((int) todayRides)
                .weekRides((int) weekRides)
                .monthRides((int) monthRides)
                .totalRides(driver.getTotalRides())
                .averageRating(driver.getRating());

        todayBonus.ifPresent(b -> builder
                .todayBonus(b.getBonusAmount())
                .bonusRidesRequired(b.getRequiredRides())
                .bonusRidesCompleted(b.getCompletedRides())
                .bonusDate(b.getTargetDate()));

        return builder.build();
    }

    @Transactional
    public void createBonusTarget(UUID driverId, int requiredRides, BigDecimal bonusAmount, LocalDate date, String description) {
        DriverBonusTarget target = DriverBonusTarget.builder()
                .driverId(driverId)
                .targetDate(date)
                .requiredRides(requiredRides)
                .bonusAmount(bonusAmount)
                .description(description)
                .build();
        bonusTargetRepository.save(target);
    }
}
