package com.oviro.controller;

import com.oviro.dto.request.QrValidationRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.QrCodeResponse;
import com.oviro.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
@Tag(name = "QR Code Paiement", description = "Génération et validation des QR codes de paiement")
@SecurityRequirement(name = "bearerAuth")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @PostMapping("/generate/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Générer un QR code pour le paiement d'une course terminée")
    public ResponseEntity<ApiResponse<QrCodeResponse>> generateQrCode(@PathVariable UUID rideId) {
        return ResponseEntity.ok(ApiResponse.ok("QR code généré", qrCodeService.generateQrCode(rideId)));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Scanner et valider un QR code de paiement")
    public ResponseEntity<ApiResponse<QrCodeResponse>> validateQrCode(
            @Valid @RequestBody QrValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Paiement effectué avec succès", qrCodeService.validateQrCode(request)));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Récupérer le QR code d'une course")
    public ResponseEntity<ApiResponse<QrCodeResponse>> getQrByRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(ApiResponse.ok(qrCodeService.getQrByRide(rideId)));
    }
}
