package com.oviro.controller;

import com.oviro.dto.request.DeliveryRequest;
import com.oviro.dto.request.RatingRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.DeliveryResponse;
import com.oviro.enums.DeliveryStatus;
import com.oviro.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
@Tag(name = "Livraison Moto", description = "Service de livraison de colis par moto")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer une demande de livraison")
    public ResponseEntity<ApiResponse<DeliveryResponse>> create(@Valid @RequestBody DeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Livraison demandée", deliveryService.createDelivery(request)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Mes livraisons (expéditeur)")
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> myDeliveries(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getMySentDeliveries(pageable)));
    }

    @GetMapping("/driver/my")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Mes livraisons (chauffeur)")
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> driverDeliveries(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getMyDriverDeliveries(pageable)));
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Livraisons disponibles")
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> available(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getAvailableDeliveries(pageable)));
    }

    @PostMapping("/{deliveryId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accepter une livraison")
    public ResponseEntity<ApiResponse<DeliveryResponse>> accept(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(ApiResponse.ok("Livraison acceptée", deliveryService.acceptDelivery(deliveryId)));
    }

    @PatchMapping("/{deliveryId}/status")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Mettre à jour le statut de livraison")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateStatus(
            @PathVariable UUID deliveryId, @RequestParam DeliveryStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Statut mis à jour", deliveryService.updateStatus(deliveryId, status)));
    }

    @PostMapping("/{deliveryId}/cancel")
    @Operation(summary = "Annuler une livraison")
    public ResponseEntity<ApiResponse<DeliveryResponse>> cancel(
            @PathVariable UUID deliveryId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Livraison annulée", deliveryService.cancel(deliveryId, reason)));
    }

    @PostMapping("/{deliveryId}/rate")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Noter une livraison")
    public ResponseEntity<ApiResponse<DeliveryResponse>> rate(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Note enregistrée", deliveryService.rateDelivery(deliveryId, request)));
    }

    @GetMapping("/{deliveryId}")
    @Operation(summary = "Détails d'une livraison")
    public ResponseEntity<ApiResponse<DeliveryResponse>> get(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getById(deliveryId)));
    }
}
