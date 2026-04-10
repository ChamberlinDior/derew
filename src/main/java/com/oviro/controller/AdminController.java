package com.oviro.controller;

import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.UserResponse;
import com.oviro.enums.UserStatus;
import com.oviro.model.SosAlert;
import com.oviro.service.AdminService;
import com.oviro.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Supervision globale OVIRO")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final SecurityContextHelper securityHelper;

    @GetMapping("/dashboard")
    @Operation(summary = "Statistiques globales du tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    @Operation(summary = "Liste paginée de tous les utilisateurs")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllUsers(pageable)));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Modifier le statut d'un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Statut mis à jour", adminService.updateUserStatus(userId, status)));
    }

    @GetMapping("/sos")
    @Operation(summary = "Alertes SOS en attente")
    public ResponseEntity<ApiResponse<Page<SosAlert>>> getPendingSos(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPendingSosAlerts(pageable)));
    }

    @PatchMapping("/sos/{alertId}/resolve")
    @Operation(summary = "Résoudre une alerte SOS")
    public ResponseEntity<ApiResponse<SosAlert>> resolveSos(
            @PathVariable UUID alertId,
            @RequestParam(required = false) String notes) {
        UUID adminId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Alerte résolue", adminService.resolveSosAlert(alertId, adminId, notes)));
    }
}
