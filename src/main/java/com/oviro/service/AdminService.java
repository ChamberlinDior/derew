package com.oviro.service;

import com.oviro.dto.response.UserResponse;
import com.oviro.enums.UserStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.SosAlert;
import com.oviro.model.User;
import com.oviro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final TransactionRepository transactionRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SosAlertRepository sosAlertRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapUser);
    }

    @Transactional
    public UserResponse updateUserStatus(UUID userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));
        user.setStatus(newStatus);
        userRepository.save(user);
        log.info("Admin: statut utilisateur {} changé à {}", userId, newStatus);
        return mapUser(user);
    }

    @Transactional
    public SosAlert resolveSosAlert(UUID alertId, UUID adminId, String notes) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte SOS", alertId.toString()));
        if (alert.isResolved()) {
            throw new BusinessException("Cette alerte est déjà résolue");
        }
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedByAdminId(adminId);
        alert.setResolutionNotes(notes);
        SosAlert saved = sosAlertRepository.save(alert);

        // Remettre le chauffeur en ligne
        var driver = saved.getDriver();
        driver.setStatus(com.oviro.enums.DriverStatus.ONLINE);
        driverProfileRepository.save(driver);

        log.info("Alerte SOS {} résolue par admin {}", alertId, adminId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<SosAlert> getPendingSosAlerts(Pageable pageable) {
        return sosAlertRepository.findByResolvedFalseOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDrivers", userRepository.countByRole(com.oviro.enums.Role.DRIVER));
        stats.put("totalClients", userRepository.countByRole(com.oviro.enums.Role.CLIENT));
        stats.put("totalRides", rideRepository.count());
        stats.put("pendingSosAlerts", sosAlertRepository.countByResolvedFalse());
        stats.put("onlineDrivers", driverProfileRepository.countByStatus(
                com.oviro.enums.DriverStatus.ONLINE));
        return stats;
    }

    private UserResponse mapUser(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole())
                .status(u.getStatus())
                .emailVerified(u.isEmailVerified())
                .phoneVerified(u.isPhoneVerified())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
