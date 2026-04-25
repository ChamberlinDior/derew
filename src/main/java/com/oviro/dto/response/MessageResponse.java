package com.oviro.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oviro.enums.MessageType;
import com.oviro.enums.Role;
import com.oviro.model.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    private UUID id;
    private UUID rideId;
    private UUID senderId;
    private String senderName;
    private String senderPhotoUrl;
    private Role senderRole;
    private UUID recipientId;
    private String content;
    private MessageType type;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;

    public static MessageResponse from(ChatMessage m) {
        return MessageResponse.builder()
                .id(m.getId())
                .rideId(m.getRide().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .senderRole(m.getSender().getRole())
                .recipientId(m.getRecipient().getId())
                .content(m.getContent())
                .type(m.getType())
                .read(m.isRead())
                .readAt(m.getReadAt())
                .sentAt(m.getSentAt())
                .build();
    }
}
