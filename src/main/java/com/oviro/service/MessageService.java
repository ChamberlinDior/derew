package com.oviro.service;

import com.oviro.dto.request.SendMessageRequest;
import com.oviro.dto.response.MessageResponse;
import com.oviro.dto.response.ReadEvent;
import com.oviro.dto.response.TypingEvent;
import com.oviro.enums.MessageType;
import com.oviro.enums.RideStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.ChatMessage;
import com.oviro.model.Ride;
import com.oviro.model.User;
import com.oviro.repository.ChatMessageRepository;
import com.oviro.repository.RideRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private static final List<RideStatus> CHAT_ACTIVE_STATUSES = List.of(
            RideStatus.DRIVER_ASSIGNED,
            RideStatus.DRIVER_EN_ROUTE,
            RideStatus.DRIVER_ARRIVED,
            RideStatus.IN_PROGRESS
    );

    private final ChatMessageRepository messageRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationService notificationService;

    @Transactional
    public MessageResponse sendMessage(UUID rideId, UUID senderId, SendMessageRequest request) {
        Ride ride = getRideOrThrow(rideId);
        validateChatOpen(ride);

        User sender = getUserOrThrow(senderId);
        User recipient = resolveRecipient(ride, senderId);

        ChatMessage msg = ChatMessage.builder()
                .ride(ride)
                .sender(sender)
                .recipient(recipient)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();

        msg = messageRepository.save(msg);
        MessageResponse response = MessageResponse.from(msg);

        messagingTemplate.convertAndSend("/topic/chat/" + rideId, response);

        // Si le destinataire n'est pas connecté → FCM
        notificationService.notifyChatMessage(
                recipient.getId(), rideId, sender.getFullName(), request.getContent());

        log.debug("Message envoyé rideId={} senderId={}", rideId, senderId);
        return response;
    }

    @Transactional
    public MessageResponse sendSystemMessage(UUID rideId, String content) {
        Ride ride = getRideOrThrow(rideId);
        User client = ride.getClient();
        User driver = ride.getDriver() != null ? ride.getDriver().getUser() : null;
        if (driver == null) return null;

        // Message système : sender=system (client), recipient=driver, mais visible des deux
        ChatMessage msg = ChatMessage.builder()
                .ride(ride)
                .sender(client)
                .recipient(driver)
                .content(content)
                .type(MessageType.SYSTEM)
                .read(true)
                .sentAt(LocalDateTime.now())
                .build();

        msg = messageRepository.save(msg);
        MessageResponse response = MessageResponse.from(msg);
        messagingTemplate.convertAndSend("/topic/chat/" + rideId, response);
        return response;
    }

    @Transactional
    public int markAsRead(UUID rideId, UUID userId) {
        int updated = messageRepository.markAsReadByRideAndRecipient(rideId, userId, LocalDateTime.now());
        if (updated > 0) {
            ReadEvent event = new ReadEvent(userId, rideId, LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/chat/" + rideId + "/read", event);
        }
        return updated;
    }

    public void sendTypingIndicator(UUID rideId, UUID userId, boolean isTyping) {
        User user = getUserOrThrow(userId);
        TypingEvent event = new TypingEvent(userId, user.getFullName(), isTyping);
        messagingTemplate.convertAndSend("/topic/chat/" + rideId + "/typing", event);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getChatHistory(UUID rideId, UUID requesterId) {
        Ride ride = getRideOrThrow(rideId);
        validateParticipant(ride, requesterId);
        return messageRepository.findByRideIdOrderBySentAtAsc(rideId)
                .stream().map(MessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID rideId, UUID userId) {
        return messageRepository.countByRideIdAndRecipientIdAndReadFalse(rideId, userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────

    private void validateChatOpen(Ride ride) {
        if (!CHAT_ACTIVE_STATUSES.contains(ride.getStatus())) {
            throw new BusinessException(
                    "Le chat est uniquement disponible quand une course est en cours",
                    "CHAT_NOT_AVAILABLE");
        }
    }

    private void validateParticipant(Ride ride, UUID userId) {
        boolean isClient = ride.getClient().getId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getUser().getId().equals(userId);
        if (!isClient && !isDriver) {
            throw new BusinessException("Vous n'êtes pas participant à cette course", "CHAT_UNAUTHORIZED");
        }
    }

    private User resolveRecipient(Ride ride, UUID senderId) {
        if (ride.getClient().getId().equals(senderId)) {
            if (ride.getDriver() == null) {
                throw new BusinessException("Aucun chauffeur assigné à cette course", "NO_DRIVER_ASSIGNED");
            }
            return ride.getDriver().getUser();
        } else if (ride.getDriver() != null && ride.getDriver().getUser().getId().equals(senderId)) {
            return ride.getClient();
        } else {
            throw new BusinessException("Vous n'êtes pas participant à cette course", "CHAT_UNAUTHORIZED");
        }
    }

    private Ride getRideOrThrow(UUID rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", rideId.toString()));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));
    }
}
