package com.oviro.controller;

import com.oviro.dto.request.PartnerProfileRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.PartnerProfileResponse;
import com.oviro.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/partner")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
@Tag(name = "Partenaires", description = "Gestion des partenaires et de leur flotte de véhicules")
@SecurityRequirement(name = "bearerAuth")
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping("/profile")
    @Operation(summary = "Profil du partenaire connecté")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(partnerService.getMyProfile()));
    }

    @PatchMapping("/profile")
    @Operation(summary = "Mettre à jour le profil partenaire")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> updateProfile(
            @Valid @RequestBody PartnerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profil mis à jour", partnerService.updateMyProfile(request)));
    }

    @GetMapping("/drivers")
    @Operation(summary = "Lister les chauffeurs de ma flotte")
    public ResponseEntity<ApiResponse<List<PartnerService.DriverSummary>>> getMyDrivers() {
        return ResponseEntity.ok(ApiResponse.ok(partnerService.getMyDrivers()));
    }

    @PostMapping("/drivers/{driverProfileId}")
    @Operation(summary = "Ajouter un chauffeur à ma flotte")
    public ResponseEntity<ApiResponse<Void>> assignDriver(@PathVariable UUID driverProfileId) {
        partnerService.assignDriverToPartner(driverProfileId);
        return ResponseEntity.ok(ApiResponse.ok("Chauffeur ajouté à la flotte", null));
    }

    @DeleteMapping("/drivers/{driverProfileId}")
    @Operation(summary = "Retirer un chauffeur de ma flotte")
    public ResponseEntity<ApiResponse<Void>> removeDriver(@PathVariable UUID driverProfileId) {
        partnerService.removeDriverFromPartner(driverProfileId);
        return ResponseEntity.ok(ApiResponse.ok("Chauffeur retiré de la flotte", null));
    }

    // --- Admin endpoints ---

    @GetMapping("/admin/partners")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister tous les partenaires (admin)")
    public ResponseEntity<ApiResponse<Page<PartnerProfileResponse>>> getAllPartners(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partnerService.getAllPartners(pageable)));
    }

    @PatchMapping("/admin/partners/{partnerId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vérifier / déverifier un partenaire (admin)")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> verify(
            @PathVariable UUID partnerId,
            @RequestParam boolean verified) {
        return ResponseEntity.ok(ApiResponse.ok(
                verified ? "Partenaire vérifié" : "Vérification annulée",
                partnerService.verifyPartner(partnerId, verified)));
    }

    @PatchMapping("/admin/partners/{partnerId}/commission")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier le taux de commission d'un partenaire (admin)")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> updateCommission(
            @PathVariable UUID partnerId,
            @RequestParam BigDecimal rate) {
        return ResponseEntity.ok(ApiResponse.ok("Taux de commission mis à jour",
                partnerService.updateCommissionRate(partnerId, rate)));
    }
}
