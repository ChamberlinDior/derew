package com.oviro.controller;

import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.ReferralResponse;
import com.oviro.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/referrals")
@RequiredArgsConstructor
@Tag(name = "Parrainage", description = "Programme de parrainage et codes de référence")
@SecurityRequirement(name = "bearerAuth")
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques de parrainage")
    public ResponseEntity<ApiResponse<ReferralResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(referralService.getMyReferralStats()));
    }

    @GetMapping
    @Operation(summary = "Liste de mes filleuls")
    public ResponseEntity<ApiResponse<Page<ReferralResponse>>> myReferrals(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(referralService.getMyReferrals(pageable)));
    }
}
