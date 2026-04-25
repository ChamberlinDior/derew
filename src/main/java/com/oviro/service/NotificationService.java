package com.oviro.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int FCM_MAX_RETRIES = 3;
    private static final long TTL_RIDE_SECONDS = 30L;
    private static final long TTL_PROMO_SECONDS = 86400L;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityContextHelper securityContextHelper;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    // ─────────────────────────────────────────────────────────────
    // Domain events (appelés par les autres services)
    // ─────────────────────────────────────────────────────────────

    public void notifyRideAccepted(Ride ride) {
        String driverName = ride.getDriver().getUser().getFullName();
        Map<String, String> data = Map.of(
                "type",        NotificationType.RIDE_ACCEPTED.name(),
                "rideId",      ride.getId().toString(),
                "reference",   ride.getReference(),
                "driverName",  driverName,
                "driverPhone", ride.getDriver().getUser().getPhoneNumber()
        );
        sendToUser(ride.getClient().getId(), NotificationType.RIDE_ACCEPTED,
                "Chauffeur trouvé !", driverName + " a accepté votre course. Il arrive bientôt.", data, TTL_RIDE_SECONDS, true);
    }

    public void notifyDriverArrived(Ride ride) {
        Map<String, String> data = Map.of(
                "type",      NotificationType.DRIVER_ARRIVED.name(),
                "rideId",    ride.getId().toString(),
                "reference", ride.getReference()
        );
        sendToUser(ride.getClient().getId(), NotificationType.DRIVER_ARRIVED,
                "Votre chauffeur est arrivé",
                ride.getDriver().getUser().getFullName() + " vous attend au point de prise en charge.",
                data, TTL_RIDE_SECONDS, true);
    }

    public void notifyRideStarted(Ride ride) {
        Map<String, String> data = Map.of(
                "type",           NotificationType.RIDE_STARTED.name(),
                "rideId",         ride.getId().toString(),
                "reference",      ride.getReference(),
                "dropoffAddress", ride.getDropoffAddress()
        );
        sendToUser(ride.getClient().getId(), NotificationType.RIDE_STARTED,
                "Course démarrée",
                "Votre course vers " + ride.getDropoffAddress() + " a commencé. Bon voyage !",
                data, TTL_RIDE_SECONDS, true);
    }

    public void notifyRideCompleted(Ride ride) {
        BigDecimal fare = ride.getActualFare() != null ? ride.getActualFare() : ride.getEstimatedFare();
        String fareStr = fare != null ? fare.toPlainString() : "—";
        Map<String, String> data = Map.of(
                "type",      NotificationType.RIDE_COMPLETED.name(),
                "rideId",    ride.getId().toString(),
                "reference", ride.getReference(),
                "amount",    fareStr
        );

        sendToUser(ride.getClient().getId(), NotificationType.RIDE_COMPLETED,
                "Course terminée",
                "Vous êtes arrivé à destination. Montant : " + fareStr + " FCFA.",
                data, TTL_RIDE_SECONDS, false);

        if (ride.getDriver() != null) {
            sendToDriver(ride.getDriver().getUser().getId(), NotificationType.RIDE_COMPLETED,
                    "Course terminée",
                    "Course " + ride.getReference() + " terminée. Gain : " + fareStr + " FCFA.",
                    data, TTL_RIDE_SECONDS, false);
        }
    }

    public void notifyRideCancelled(Ride ride, boolean cancelledByClient) {
        String title = "Course annulée";
        String body = cancelledByClient
                ? "Le passager a annulé la course " + ride.getReference()
                : "Le chauffeur a annulé la course " + ride.getReference();
        Map<String, String> data = Map.of(
                "type",      NotificationType.RIDE_CANCELLED.name(),
                "rideId",    ride.getId().toString(),
                "reference", ride.getReference()
        );

        UUID notifyId = cancelledByClient
                ? (ride.getDriver() != null ? ride.getDriver().getUser().getId() : null)
                : ride.getClient().getId();

        if (notifyId != null) {
            sendToUser(notifyId, NotificationType.RIDE_CANCELLED, title, body, data, TTL_RIDE_SECONDS, true);
        }
    }

    public void notifySosAlertToAdmins(SosAlert alert) {
        String driverName = alert.getDriver().getUser().getFullName();
        String title = "Alerte SOS Chauffeur";
        String body = driverName + " a déclenché une alerte SOS. Coordonnées : "
                + alert.getLatitude() + ", " + alert.getLongitude();
        Map<String, String> data = Map.of(
                "type",      NotificationType.SOS_ALERT.name(),
                "alertId",   alert.getId().toString(),
                "driverId",  alert.getDriver().getId().toString(),
                "latitude",  alert.getLatitude().toPlainString(),
                "longitude", alert.getLongitude().toPlainString()
        );

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        for (User admin : admins) {
            sendToUser(admin.getId(), NotificationType.SOS_ALERT, title, body, data, TTL_RIDE_SECONDS, true);
        }

        NotificationResponse payload = buildPayload(NotificationType.SOS_ALERT, title, body, data);
        messagingTemplate.convertAndSend("/topic/sos-alerts", payload);
        sendToTopic("admins", title, body, data, TTL_RIDE_SECONDS, true);
        log.info("SOS broadcasté sur /topic/sos-alerts pour alertId={}", alert.getId());
    }

    public void notifyWalletRecharged(UUID userId, BigDecimal amount, BigDecimal newBalance) {
        Map<String, String> data = Map.of(
                "type",       NotificationType.WALLET_RECHARGED.name(),
                "amount",     amount.toPlainString(),
                "newBalance", newBalance.toPlainString()
        );
        sendToUser(userId, NotificationType.WALLET_RECHARGED,
                "Recharge confirmée",
                "Votre wallet a été crédité de " + amount.toPlainString() + " UT. Nouveau solde : " + newBalance.toPlainString() + " UT.",
                data, TTL_PROMO_SECONDS, false);
    }

    public void notifyWalletTransfer(UUID senderId, UUID recipientId, BigDecimal amount, String senderName) {
        Map<String, String> senderData = Map.of(
                "type",   NotificationType.WALLET_TRANSFER.name(),
                "amount", amount.toPlainString()
        );
        Map<String, String> recipientData = Map.of(
                "type",       NotificationType.WALLET_TRANSFER.name(),
                "amount",     amount.toPlainString(),
                "senderName", senderName
        );
        sendToUser(senderId, NotificationType.WALLET_TRANSFER,
                "Transfert envoyé", "Vous avez transféré " + amount + " UT.",
                senderData, TTL_PROMO_SECONDS, false);
        sendToUser(recipientId, NotificationType.WALLET_TRANSFER,
                "Transfert reçu", senderName + " vous a envoyé " + amount + " UT.",
                recipientData, TTL_PROMO_SECONDS, false);
    }

    public void notifyChatMessage(UUID recipientId, UUID rideId, String senderName, String preview) {
        Map<String, String> data = Map.of(
                "type",       NotificationType.NEW_MESSAGE.name(),
                "rideId",     rideId.toString(),
                "senderName", senderName,
                "preview",    preview.length() > 50 ? preview.substring(0, 50) + "…" : preview
        );
        sendToUser(recipientId, NotificationType.NEW_MESSAGE,
                "Message de " + senderName,
                preview,
                data, TTL_RIDE_SECONDS, true);
    }

    // ─────────────────────────────────────────────────────────────
    // API publique enrichie
    // ─────────────────────────────────────────────────────────────

    public com.oviro.model.Notification sendToUser(
            UUID userId, NotificationType type, String title, String body,
            Map<String, String> data, long ttlSeconds, boolean highPriority) {

        com.oviro.model.Notification notification = persistNotificationBestEffort(userId, type, title, body, data);
        NotificationResponse payload = notification != null
                ? NotificationResponse.from(notification)
                : buildPayload(type, title, body, data);

        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
        sendFcmToUser(userId, title, body, data, ttlSeconds, highPriority);
        return notification;
    }

    public com.oviro.model.Notification sendToDriver(
            UUID driverUserId, NotificationType type, String title, String body,
            Map<String, String> data, long ttlSeconds, boolean highPriority) {
        return sendToUser(driverUserId, type, title, body, data, ttlSeconds, highPriority);
    }

    @Async
    public void sendToTopic(String topic, String title, String body,
                            Map<String, String> data, long ttlSeconds, boolean highPriority) {
        if (FirebaseApp.getApps().isEmpty()) return;
        try {
            com.google.firebase.messaging.Message msg = buildTopicMessage(topic, title, body, data, ttlSeconds, highPriority);
            String response = FirebaseMessaging.getInstance().send(msg);
            log.debug("FCM topic={} envoyé messageId={}", topic, response);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM topic={} échoué: {}", topic, e.getMessage());
        }
    }

    @Async
    public void sendBulk(List<UUID> userIds, String title, String body, NotificationType type) {
        if (userIds == null || userIds.isEmpty()) return;
        List<String> tokens = new ArrayList<>();
        for (UUID uid : userIds) {
            userRepository.findById(uid).ifPresent(u -> {
                if (u.getFcmToken() != null && !u.getFcmToken().isBlank()) {
                    tokens.add(u.getFcmToken());
                }
            });
        }
        if (tokens.isEmpty() || FirebaseApp.getApps().isEmpty()) return;

        MulticastMessage msg = MulticastMessage.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title).setBody(body).build())
                .addAllTokens(tokens)
                .build();
        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(msg);
            log.info("FCM bulk: {}/{} envoyés", resp.getSuccessCount(), tokens.size());
        } catch (FirebaseMessagingException e) {
            log.warn("FCM bulk échoué: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REST helpers (NotificationController)
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
        subscribeToUserTopic(userId, user.getFcmToken());
        log.debug("FCM token enregistré pour userId={}", userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Méthode de compatibilité (conservée pour les anciens appels)
    // ─────────────────────────────────────────────────────────────

    public com.oviro.model.Notification createAndSend(
            UUID userId, NotificationType type, String title, String body,
            Map<String, String> data) {
        return sendToUser(userId, type, title, body, data, TTL_PROMO_SECONDS, false);
    }

    // ─────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────

    private com.oviro.model.Notification createAndSave(
            UUID userId, NotificationType type, String title, String body,
            Map<String, String> data) {
        com.oviro.model.Notification notification = com.oviro.model.Notification.builder()
                .userId(userId).type(type).title(title).body(body).data(toJson(data)).build();
        return notificationRepository.save(notification);
    }

    private com.oviro.model.Notification persistNotificationBestEffort(
            UUID userId, NotificationType type, String title, String body,
            Map<String, String> data) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            return template.execute(status -> createAndSave(userId, type, title, body, data));
        } catch (Exception e) {
            log.error("Notification persistée en échec userId={} type={} — envoi temps réel conservé",
                    userId, type, e);
            return null;
        }
    }

    @Async
    protected void sendFcmToUser(UUID userId, String title, String body,
                                 Map<String, String> data, long ttlSeconds, boolean highPriority) {
        if (FirebaseApp.getApps().isEmpty()) return;
        userRepository.findById(userId).ifPresent(user -> {
            String token = user.getFcmToken();
            if (token == null || token.isBlank()) return;
            sendWithRetry(token, userId, title, body, data, ttlSeconds, highPriority, 0);
        });
    }

    private void sendWithRetry(String token, UUID userId, String title, String body,
                               Map<String, String> data, long ttlSeconds, boolean highPriority, int attempt) {
        try {
            com.google.firebase.messaging.Message msg = buildTokenMessage(
                    token, title, body, data, ttlSeconds, highPriority);
            String response = FirebaseMessaging.getInstance().send(msg);
            log.debug("FCM push userId={} messageId={} attempt={}", userId, response, attempt + 1);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn("FCM token invalide pour userId={} — suppression", userId);
                clearInvalidToken(userId);
            } else if (attempt < FCM_MAX_RETRIES - 1) {
                log.warn("FCM retry {}/{} pour userId={}: {}", attempt + 1, FCM_MAX_RETRIES, userId, e.getMessage());
                sendWithRetry(token, userId, title, body, data, ttlSeconds, highPriority, attempt + 1);
            } else {
                log.error("FCM échoué après {} tentatives pour userId={}: {}", FCM_MAX_RETRIES, userId, e.getMessage());
            }
        }
    }

    private void clearInvalidToken(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFcmToken(null);
            userRepository.save(user);
        });
    }

    private com.google.firebase.messaging.Message buildTokenMessage(
            String token, String title, String body, Map<String, String> data,
            long ttlSeconds, boolean highPriority) {

        com.google.firebase.messaging.Message.Builder builder = com.google.firebase.messaging.Message.builder()
                .setToken(token)
                // notification payload → foreground display
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title).setBody(body).build())
                // data payload → background handling
                .putAllData(data != null ? data : Map.of())
                .putData("title", title)
                .putData("body", body)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(highPriority ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL)
                        .setTtl(ttlSeconds * 1000)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setContentAvailable(true)
                                .setSound("default")
                                .build())
                        .build());

        return builder.build();
    }

    private com.google.firebase.messaging.Message buildTopicMessage(
            String topic, String title, String body, Map<String, String> data,
            long ttlSeconds, boolean highPriority) {

        return com.google.firebase.messaging.Message.builder()
                .setTopic(topic)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title).setBody(body).build())
                .putAllData(data != null ? data : Map.of())
                .putData("title", title)
                .putData("body", body)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(highPriority ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL)
                        .setTtl(ttlSeconds * 1000)
                        .build())
                .build();
    }

    private void subscribeToUserTopic(UUID userId, String token) {
        if (FirebaseApp.getApps().isEmpty() || token == null || token.isBlank()) return;
        String topic = "user-" + userId.toString();
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(List.of(token), topic);
            log.debug("Abonné userId={} au topic={}", userId, topic);
        } catch (FirebaseMessagingException e) {
            log.warn("Abonnement topic échoué userId={}: {}", userId, e.getMessage());
        }
    }

    private NotificationResponse buildPayload(NotificationType type, String title, String body, Map<String, String> data) {
        return NotificationResponse.builder()
                .type(type).title(title).body(body)
                .data(toJson(data)).createdAt(LocalDateTime.now()).build();
    }

    private String toJson(Map<String, String> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser les données JSON", e);
            return null;
        }
    }
}
