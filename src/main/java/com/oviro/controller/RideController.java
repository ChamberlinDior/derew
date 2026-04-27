package com.oviro.controller;

import com.oviro.dto.request.ClientSosRequest;
import com.oviro.dto.request.RatingRequest;
import com.oviro.dto.request.RideRequest;
import com.oviro.dto.request.TipRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.RideEstimateResponse;
import com.oviro.dto.response.RideResponse;
import com.oviro.dto.response.SurgeInfoResponse;
import com.oviro.enums.RideStatus;
import com.oviro.enums.ServiceType;
import com.oviro.service.RideService;
import com.oviro.service.SurgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Gestion du cycle de vie des courses")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;
    private final SurgeService surgeService;

    @GetMapping("/estimate")
    @Operation(summary = "Estimer le prix d'une course")
    public ResponseEntity<ApiResponse<RideEstimateResponse>> estimate(
            @RequestParam BigDecimal fromLat,
            @RequestParam BigDecimal fromLng,
            @RequestParam BigDecimal toLat,
            @RequestParam BigDecimal toLng,
            @RequestParam(required = false, defaultValue = "STANDARD") ServiceType serviceType) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideService.estimateRide(fromLat, fromLng, toLat, toLng, serviceType)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Demander une course")
    public ResponseEntity<ApiResponse<RideResponse>> requestRide(@Valid @RequestBody RideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course demandée avec succès", rideService.requestRide(request)));
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lister les courses disponibles pour les chauffeurs")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getAvailableRides(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(rideService.getAvailableRides(lat, lng, pageable))
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Historique des courses du client connecté")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getMyRides(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.getMyRides(pageable)));
    }

    @GetMapping("/driver/my")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Historique des courses du chauffeur connecté")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getDriverRides(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.getDriverRides(pageable)));
    }

    @PostMapping("/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accepter une course (chauffeur)")
    public ResponseEntity<ApiResponse<RideResponse>> acceptRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(ApiResponse.ok("Course acceptée", rideService.acceptRide(rideId)));
    }

    @PatchMapping("/{rideId}/status")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Mettre à jour le statut d'une course")
    public ResponseEntity<ApiResponse<RideResponse>> updateStatus(
            @PathVariable UUID rideId,
            @RequestParam RideStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Statut mis à jour", rideService.updateStatus(rideId, status)));
    }

    @PostMapping("/{rideId}/cancel")
    @Operation(summary = "Annuler une course")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(
            @PathVariable UUID rideId,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Course annulée", rideService.cancelRide(rideId, reason)));
    }

    @PostMapping("/{rideId}/rate")
    @Operation(summary = "Noter une course")
    public ResponseEntity<ApiResponse<RideResponse>> rateRide(
            @PathVariable UUID rideId,
            @Valid @RequestBody RatingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Notation enregistrée", rideService.rateRide(rideId, request)));
    }

    @GetMapping("/{rideId:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Détails d'une course")
    public ResponseEntity<ApiResponse<RideResponse>> getRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.getRideById(rideId)));
    }

    @GetMapping("/share/{shareToken}")
    @Operation(summary = "Suivre une course via token de partage (public)")
    public ResponseEntity<ApiResponse<RideResponse>> getRideByShareToken(@PathVariable String shareToken) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.getRideByShareToken(shareToken)));
    }

    @PostMapping("/{rideId}/sos")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Déclencher une alerte SOS passager")
    public ResponseEntity<ApiResponse<Void>> clientSos(
            @PathVariable UUID rideId,
            @Valid @RequestBody ClientSosRequest request) {
        rideService.triggerClientSos(rideId, request);
        return ResponseEntity.ok(ApiResponse.ok("Alerte SOS envoyée", null));
    }

    @PostMapping("/{rideId}/tip")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Donner un pourboire au chauffeur")
    public ResponseEntity<ApiResponse<Void>> tip(
            @PathVariable UUID rideId,
            @Valid @RequestBody TipRequest request) {
        rideService.addTip(rideId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.ok("Pourboire envoyé, merci !", null));
    }

    @GetMapping("/estimate/all")
    @Operation(summary = "Estimer le prix pour tous les types de service")
    public ResponseEntity<ApiResponse<java.util.Map<ServiceType, RideEstimateResponse>>> estimateAll(
            @RequestParam BigDecimal fromLat,
            @RequestParam BigDecimal fromLng,
            @RequestParam BigDecimal toLat,
            @RequestParam BigDecimal toLng) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.estimateAllServiceTypes(fromLat, fromLng, toLat, toLng)));
    }
}