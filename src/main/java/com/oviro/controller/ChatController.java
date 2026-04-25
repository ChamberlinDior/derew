package com.oviro.controller;

import com.oviro.dto.request.SendMessageRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.MessageResponse;
import com.oviro.service.MessageService;
import com.oviro.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/rides/{rideId}/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Messagerie temps réel chauffeur / passager")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final MessageService messageService;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping("/send")
    @Operation(summary = "Envoyer un message")
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @PathVariable UUID rideId,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        MessageResponse msg = messageService.sendMessage(rideId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Message envoyé", msg));
    }

    @GetMapping("/history")
    @Operation(summary = "Historique des messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> history(@PathVariable UUID rideId) {
        UUID userId = securityContextHelper.getCurrentUserId();
        List<MessageResponse> messages = messageService.getChatHistory(rideId, userId);
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @PutMapping("/read")
    @Operation(summary = "Marquer les messages comme lus")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAsRead(@PathVariable UUID rideId) {
        UUID userId = securityContextHelper.getCurrentUserId();
        int updated = messageService.markAsRead(rideId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Messages marqués comme lus", Map.of("updated", updated)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de messages non lus")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@PathVariable UUID rideId) {
        UUID userId = securityContextHelper.getCurrentUserId();
        long count = messageService.getUnreadCount(rideId, userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    // ─────────────────────────────────────────────────────────────
    // WebSocket @MessageMapping
    // ─────────────────────────────────────────────────────────────

    @MessageMapping("/chat/{rideId}")
    public void wsMessage(@DestinationVariable UUID rideId,
                          @Payload SendMessageRequest request,
                          Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        messageService.sendMessage(rideId, userId, request);
    }

    @MessageMapping("/chat/{rideId}/typing")
    public void wsTyping(@DestinationVariable UUID rideId,
                         @Payload Map<String, Boolean> payload,
                         Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));
        messageService.sendTypingIndicator(rideId, userId, isTyping);
    }

    @MessageMapping("/chat/{rideId}/read")
    public void wsRead(@DestinationVariable UUID rideId, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        messageService.markAsRead(rideId, userId);
    }
}
