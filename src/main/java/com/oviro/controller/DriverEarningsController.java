package com.oviro.controller;

import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.DriverEarningsResponse;
import com.oviro.service.DriverEarningsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver/earnings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
@Tag(name = "Gains Chauffeur", description = "Tableau de bord des gains et objectifs du chauffeur")
@SecurityRequirement(name = "bearerAuth")
public class DriverEarningsController {

    private final DriverEarningsService driverEarningsService;

    @GetMapping
    @Operation(summary = "Mes gains (aujourd'hui, semaine, mois, total)")
    public ResponseEntity<ApiResponse<DriverEarningsResponse>> getEarnings() {
        return ResponseEntity.ok(ApiResponse.ok(driverEarningsService.getMyEarnings()));
    }
}
