package com.oviro.controller;

import com.oviro.dto.request.FCMTokenRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.NotificationResponse;
import com.oviro.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Historique et gestion des notifications utilisateur")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lister mes notifications (paginées, plus récentes en premier)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getMyNotifications(pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Notification marquée comme lue", notificationService.markAsRead(id))
        );
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(
                ApiResponse.ok("Toutes les notifications marquées comme lues", Map.of("updated", updated))
        );
    }

    @PostMapping("/fcm-token")
    @Operation(summary = "Enregistrer le token FCM du device mobile")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(@Valid @RequestBody FCMTokenRequest request) {
        notificationService.registerFcmToken(request.getFcmToken());
        return ResponseEntity.ok(ApiResponse.ok("Token FCM enregistré", null));
    }
}
