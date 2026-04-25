package com.oviro.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.oviro.dto.response.NotificationResponse;
import com.oviro.enums.NotificationType;
import com.oviro.enums.Role;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.Ride;
import com.oviro.model.SosAlert;
import com.oviro.model.User;
import com.oviro.repository.NotificationRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityContextHelper securityContextHelper;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // Domain event methods (called by other services)
    // ─────────────────────────────────────────────────────────────

    /** Chauffeur accepte une course → notifier le passager. */
    public void notifyRideAccepted(Ride ride) {
        String driverName = ride.getDriver().getUser().getFullName();
        String title = "Chauffeur trouvé !";
        String body  = driverName + " a accepté votre course. Il arrive bientôt.";
        Map<String, String> data = Map.of(
                "rideId",     ride.getId().toString(),
                "reference",  ride.getReference(),
                "driverName", driverName,
                "driverPhone", ride.getDriver().getUser().getPhoneNumber()
        );

        UUID passengerId = ride.getClient().getId();
        createAndSend(passengerId, NotificationType.RIDE_ACCEPTED, title, body, data);
    }

    /** Chauffeur arrive au point de pickup → notifier le passager. */
    public void notifyDriverArrived(Ride ride) {
        String title = "Votre chauffeur est arrivé";
        String body  = ride.getDriver().getUser().getFullName() + " vous attend au point de prise en charge.";
        Map<String, String> data = Map.of(
                "rideId",    ride.getId().toString(),
                "reference", ride.getReference()
        );

        createAndSend(ride.getClient().getId(), NotificationType.DRIVER_ARRIVED, title, body, data);
    }

    /** Course démarrée → notifier le passager. */
    public void notifyRideStarted(Ride ride) {
        String title = "Course démarrée";
        String body  = "Votre course vers " + ride.getDropoffAddress() + " a commencé. Bon voyage !";
        Map<String, String> data = Map.of(
                "rideId",          ride.getId().toString(),
                "reference",       ride.getReference(),
                "dropoffAddress",  ride.getDropoffAddress()
        );

        createAndSend(ride.getClient().getId(), NotificationType.RIDE_STARTED, title, body, data);
    }

    /** Course terminée → notifier passager ET chauffeur (avec prix). */
    public void notifyRideCompleted(Ride ride) {
        BigDecimal fare = ride.getActualFare() != null ? ride.getActualFare() : ride.getEstimatedFare();
        String fareStr  = fare != null ? fare.toPlainString() : "—";

        // Passenger
        String passengerTitle = "Course terminée";
        String passengerBody  = "Vous êtes arrivé à destination. Montant : " + fareStr + " XAF.";
        Map<String, String> passengerData = Map.of(
                "rideId",    ride.getId().toString(),
                "reference", ride.getReference(),
                "amount",    fareStr
        );
        createAndSend(ride.getClient().getId(), NotificationType.RIDE_COMPLETED, passengerTitle, passengerBody, passengerData);

        // Driver
        if (ride.getDriver() != null) {
            String driverTitle = "Course terminée";
            String driverBody  = "Course " + ride.getReference() + " terminée. Gain : " + fareStr + " XAF.";
            createAndSend(ride.getDriver().getUser().getId(), NotificationType.RIDE_COMPLETED, driverTitle, driverBody, passengerData);
        }
    }

    /** Nouveau SOS → notifier tous les admins. */
    public void notifySosAlertToAdmins(SosAlert alert) {
        String driverName = alert.getDriver().getUser().getFullName();
        String title = "🚨 Alerte SOS Chauffeur";
        String body  = driverName + " a déclenché une alerte SOS. Coordonnées : "
                + alert.getLatitude() + ", " + alert.getLongitude();
        Map<String, String> data = Map.of(
                "alertId",   alert.getId().toString(),
                "driverId",  alert.getDriver().getId().toString(),
                "latitude",  alert.getLatitude().toPlainString(),
                "longitude", alert.getLongitude().toPlainString()
        );

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        for (User admin : admins) {
            createAndSend(admin.getId(), NotificationType.SOS_ALERT, title, body, data);
        }

        // Broadcast on the admin topic as well (for admin dashboards listening via WebSocket)
        NotificationResponse payload = buildPayload(NotificationType.SOS_ALERT, title, body, data);
        messagingTemplate.convertAndSend("/topic/sos-alerts", payload);
        log.info("SOS alerte broadcastée sur /topic/sos-alerts pour alertId={}", alert.getId());
    }

    /** Recharge wallet confirmée → notifier l'utilisateur. */
    public void notifyWalletRecharged(UUID userId, BigDecimal amount, BigDecimal newBalance) {
        String title = "Recharge confirmée";
        String body  = "Votre wallet a été crédité de " + amount.toPlainString()
                + " XAF. Nouveau solde : " + newBalance.toPlainString() + " XAF.";
        Map<String, String> data = Map.of(
                "amount",     amount.toPlainString(),
                "newBalance", newBalance.toPlainString()
        );

        createAndSend(userId, NotificationType.WALLET_RECHARGED, title, body, data);
    }

    // ─────────────────────────────────────────────────────────────
    // REST helpers (called by NotificationController)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        UUID userId = securityContextHelper.getCurrentUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID userId = securityContextHelper.getCurrentUserId();
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UUID userId = securityContextHelper.getCurrentUserId();
        com.oviro.model.Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId.toString()));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead() {
        UUID userId = securityContextHelper.getCurrentUserId();
        return notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public void registerFcmToken(String fcmToken) {
        UUID userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.debug("FCM token enregistré pour userId={}", userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Internal core
    // ─────────────────────────────────────────────────────────────

    /**
     * Persists the notification, delivers it via WebSocket in real-time,
     * and (if configured) pushes it via FCM.
     */
    @Transactional
    public com.oviro.model.Notification createAndSend(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            Map<String, String> data
    ) {
        String dataJson = toJson(data);

        com.oviro.model.Notification notification = com.oviro.model.Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .data(dataJson)
                .build();

        notification = notificationRepository.save(notification);

        NotificationResponse payload = NotificationResponse.from(notification);

        // WebSocket: push to the specific user's private queue
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
        log.debug("Notification WS envoyée → userId={} type={}", userId, type);

        // FCM: best-effort push (doesn't break the transaction if it fails)
        sendFcmIfPossible(userId, title, body, data);

        return notification;
    }

    private void sendFcmIfPossible(UUID userId, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            return; // Firebase not configured
        }

        userRepository.findById(userId).ifPresent(user -> {
            String token = user.getFcmToken();
            if (token == null || token.isBlank()) {
                return;
            }

            try {
                Message fcmMessage = Message.builder()
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data)
                        .setToken(token)
                        .build();

                String response = FirebaseMessaging.getInstance().send(fcmMessage);
                log.debug("FCM push envoyé → userId={} messageId={}", userId, response);

            } catch (FirebaseMessagingException e) {
                // Log and continue – push failure must not roll back the main transaction
                log.warn("FCM push échoué pour userId={}: {}", userId, e.getMessage());
            }
        });
    }

    private NotificationResponse buildPayload(NotificationType type, String title, String body, Map<String, String> data) {
        return NotificationResponse.builder()
                .type(type)
                .title(title)
                .body(body)
                .data(toJson(data))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String toJson(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les données de notification en JSON", e);
            return null;
        }
    }
}
