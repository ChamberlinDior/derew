package com.oviro.service;

import com.oviro.dto.response.UserResponse;
import com.oviro.enums.*;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
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
import java.util.*;

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
    private final NotificationService notificationService;
    private final DeliveryRepository deliveryRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final SupportTicketRepository supportTicketRepository;

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
    public UserResponse approveDriver(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        DriverProfile dp = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Cet utilisateur n'a pas de profil chauffeur"));

        dp.setVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        driverProfileRepository.save(dp);
        userRepository.save(user);

        notificationService.sendToUser(userId, NotificationType.DRIVER_APPROVED,
                "Compte chauffeur approuvé",
                "Félicitations ! Votre compte chauffeur OVIRO a été approuvé. Vous pouvez maintenant accepter des courses.",
                Map.of("type", NotificationType.DRIVER_APPROVED.name()),
                86400L, false);

        log.info("Chauffeur {} approuvé", userId);
        return mapUser(user);
    }

    @Transactional
    public UserResponse suspendDriver(UUID userId) {
        return updateUserStatus(userId, UserStatus.SUSPENDED);
    }

    @Transactional
    public UserResponse activateDriver(UUID userId) {
        return updateUserStatus(userId, UserStatus.ACTIVE);
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

        var driver = saved.getDriver();
        driver.setStatus(DriverStatus.ONLINE);
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
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDrivers", userRepository.countByRole(Role.DRIVER));
        stats.put("totalClients", userRepository.countByRole(Role.CLIENT));
        stats.put("totalRides", rideRepository.count());
        stats.put("activeRidesNow", rideRepository.countByStatusIn(List.of(
                RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_EN_ROUTE,
                RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS)));
        stats.put("activeDrivers", driverProfileRepository.countByStatus(DriverStatus.ONLINE));
        stats.put("pendingSosAlerts", sosAlertRepository.countByResolvedFalse());
        stats.put("totalTransactions", transactionRepository.count());
        stats.put("totalDeliveries", deliveryRepository.count());
        stats.put("totalFoodOrders", foodOrderRepository.count());
        stats.put("openSupportTickets", supportTicketRepository.countByStatus(com.oviro.enums.SupportTicketStatus.OPEN));
        return stats;
    }

    public void broadcastNotification(String title, String body, String targetType,
                                      List<UUID> targetUserIds) {
        switch (targetType.toUpperCase()) {
            case "ALL" -> {
                notificationService.sendToTopic("all-users", title, body,
                        Map.of("type", "PROMO_OFFER"), 86400L, false);
            }
            case "DRIVERS" -> {
                notificationService.sendToTopic("all-drivers", title, body,
                        Map.of("type", "PROMO_OFFER"), 86400L, false);
            }
            case "PASSENGERS" -> {
                notificationService.sendToTopic("all-passengers", title, body,
                        Map.of("type", "PROMO_OFFER"), 86400L, false);
            }
            case "USERS" -> {
                if (targetUserIds != null) {
                    notificationService.sendBulk(targetUserIds, title, body, NotificationType.PROMO_OFFER);
                }
            }
            default -> log.warn("Type de broadcast inconnu: {}", targetType);
        }
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
