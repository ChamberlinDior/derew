package com.oviro.controller;

import com.oviro.dto.request.RatingRequest;
import com.oviro.dto.request.RideRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.RideResponse;
import com.oviro.enums.RideStatus;
import com.oviro.service.RideService;
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
@RequestMapping("/rides")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Gestion du cycle de vie des courses")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Demander une course")
    public ResponseEntity<ApiResponse<RideResponse>> requestRide(@Valid @RequestBody RideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course demandée avec succès", rideService.requestRide(request)));
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Détails d'une course")
    public ResponseEntity<ApiResponse<RideResponse>> getRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.getRideById(rideId)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Historique des courses du client connecté")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getMyRides(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.getMyRides(pageable)));
    }

    @GetMapping("/driver/my")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Historique des courses du chauffeur connecté")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getDriverRides(
            @PageableDefault(size = 20) Pageable pageable) {
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
            @RequestParam RideStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Statut mis à jour", rideService.updateStatus(rideId, status)));
    }

    @PostMapping("/{rideId}/cancel")
    @Operation(summary = "Annuler une course")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(
            @PathVariable UUID rideId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Course annulée", rideService.cancelRide(rideId, reason)));
    }

    @PostMapping("/{rideId}/rate")
    @Operation(summary = "Noter une course")
    public ResponseEntity<ApiResponse<RideResponse>> rateRide(
            @PathVariable UUID rideId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Notation enregistrée", rideService.rateRide(rideId, request)));
    }
}
