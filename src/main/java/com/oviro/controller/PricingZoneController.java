package com.oviro.controller;

import com.oviro.dto.request.PricingZoneRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.PricingZoneResponse;
import com.oviro.service.SurgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/pricing-zones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Zones de Tarification", description = "Gestion des zones géographiques et surge pricing")
@SecurityRequirement(name = "bearerAuth")
public class PricingZoneController {

    private final SurgeService surgeService;

    @GetMapping
    @Operation(summary = "Toutes les zones")
    public ResponseEntity<ApiResponse<List<PricingZoneResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.ok(surgeService.getAllZones()));
    }

    @PostMapping
    @Operation(summary = "Créer une zone")
    public ResponseEntity<ApiResponse<PricingZoneResponse>> create(
            @Valid @RequestBody PricingZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Zone créée", surgeService.createZone(request)));
    }

    @PutMapping("/{zoneId}")
    @Operation(summary = "Modifier une zone")
    public ResponseEntity<ApiResponse<PricingZoneResponse>> update(
            @PathVariable UUID zoneId,
            @Valid @RequestBody PricingZoneRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Zone mise à jour", surgeService.updateZone(zoneId, request)));
    }

    @DeleteMapping("/{zoneId}")
    @Operation(summary = "Supprimer une zone")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID zoneId) {
        surgeService.deleteZone(zoneId);
        return ResponseEntity.ok(ApiResponse.ok("Zone supprimée", null));
    }
}
