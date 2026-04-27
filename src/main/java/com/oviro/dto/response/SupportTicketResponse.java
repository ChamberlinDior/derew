package com.oviro.dto.response;

import com.oviro.enums.SupportTicketCategory;
import com.oviro.enums.SupportTicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SupportTicketResponse {
    private UUID id;
    private String reference;
    private UUID userId;
    private SupportTicketCategory category;
    private String subject;
    private SupportTicketStatus status;
    private UUID relatedRideId;
    private UUID relatedDeliveryId;
    private UUID relatedOrderId;
    private List<SupportMessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class SupportMessageResponse {
        private UUID id;
        private UUID senderId;
        private String senderName;
        private String content;
        private boolean fromAgent;
        private LocalDateTime createdAt;
    }
}
