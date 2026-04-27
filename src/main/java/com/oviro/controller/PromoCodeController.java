package com.oviro.controller;

import com.oviro.dto.request.ApplyPromoCodeRequest;
import com.oviro.dto.request.PromoCodeRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.PromoCodeResponse;
import com.oviro.service.PromoCodeService;
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
@RequestMapping("/promo-codes")
@RequiredArgsConstructor
@Tag(name = "Codes Promo", description = "Gestion et application des codes de réduction")
@SecurityRequirement(name = "bearerAuth")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping("/active")
    @Operation(summary = "Codes promo actifs disponibles")
    public ResponseEntity<ApiResponse<Page<PromoCodeResponse>>> getActive(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(promoCodeService.getActivePromos(pageable)));
    }

    @PostMapping("/apply")
    @Operation(summary = "Vérifier et appliquer un code promo")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> apply(@Valid @RequestBody ApplyPromoCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promoCodeService.applyCode(request)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un code promo [Admin]")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> create(@Valid @RequestBody PromoCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Code promo créé", promoCodeService.createCode(request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Désactiver un code promo [Admin]")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        promoCodeService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Code désactivé", null));
    }
}
