package com.oviro.controller;

import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.NearbyDriverResponse;
import com.oviro.dto.response.SurgeInfoResponse;
import com.oviro.service.SurgeService;
import com.oviro.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking & Surge", description = "Positions des chauffeurs en temps réel et zones de surge pricing")
@SecurityRequirement(name = "bearerAuth")
public class TrackingController {

    private final TrackingService trackingService;
    private final SurgeService surgeService;

    @GetMapping("/drivers/nearby")
    @Operation(summary = "Chauffeurs disponibles à proximité")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> nearbyDrivers(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        return ResponseEntity.ok(ApiResponse.ok(trackingService.getNearbyDrivers(lat, lng, radiusKm)));
    }

    @GetMapping("/surge")
    @Operation(summary = "Informations de surge pricing pour une position")
    public ResponseEntity<ApiResponse<SurgeInfoResponse>> getSurge(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {
        return ResponseEntity.ok(ApiResponse.ok(surgeService.getSurgeInfo(lat, lng)));
    }
}
