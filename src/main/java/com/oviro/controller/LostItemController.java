package com.oviro.controller;

import com.oviro.dto.request.LostItemReportRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.LostItemReportResponse;
import com.oviro.service.LostItemService;
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
@RequestMapping("/lost-items")
@RequiredArgsConstructor
@Tag(name = "Objets Perdus", description = "Signalement et suivi des objets oubliés")
@SecurityRequirement(name = "bearerAuth")
public class LostItemController {

    private final LostItemService lostItemService;

    @PostMapping
    @Operation(summary = "Signaler un objet oublié")
    public ResponseEntity<ApiResponse<LostItemReportResponse>> report(
            @Valid @RequestBody LostItemReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Signalement enregistré", lostItemService.report(request)));
    }

    @GetMapping("/my")
    @Operation(summary = "Mes signalements")
    public ResponseEntity<ApiResponse<Page<LostItemReportResponse>>> myReports(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(lostItemService.getMyReports(pageable)));
    }

    @GetMapping("/admin/unresolved")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Signalements non résolus [Admin]")
    public ResponseEntity<ApiResponse<Page<LostItemReportResponse>>> unresolved(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(lostItemService.getAllUnresolved(pageable)));
    }

    @PatchMapping("/admin/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Résoudre un signalement [Admin]")
    public ResponseEntity<ApiResponse<LostItemReportResponse>> resolve(
            @PathVariable UUID reportId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(ApiResponse.ok("Résolu", lostItemService.resolve(reportId, notes)));
    }
}
