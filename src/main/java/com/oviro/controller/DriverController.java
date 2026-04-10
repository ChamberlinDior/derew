package com.oviro.controller;

import com.oviro.dto.request.DriverLocationRequest;
import com.oviro.dto.request.SosAlertRequest;
import com.oviro.dto.request.UpdateDriverProfileRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.DriverProfileResponse;
import com.oviro.dto.response.UploadDriverProfilePhotoResponse;
import com.oviro.model.DriverProfile;
import com.oviro.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/driver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
@Tag(name = "Chauffeur", description = "Gestion du statut, localisation et alertes chauffeur")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/profile")
    @Operation(summary = "Profil du chauffeur connecté")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(driverService.getMyProfile()));
    }

    @PatchMapping("/profile")
    @Operation(summary = "Mettre à jour le profil du chauffeur connecté")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateDriverProfileRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok("Profil chauffeur mis à jour", driverService.updateMyProfile(request))
        );
    }

    @PostMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader la photo de profil du chauffeur connecté")
    public ResponseEntity<ApiResponse<UploadDriverProfilePhotoResponse>> uploadProfilePhoto(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok("Photo de profil mise à jour", driverService.uploadProfilePhoto(file))
        );
    }

    @GetMapping("/profile/photo")
    @Operation(summary = "Lire la photo de profil du chauffeur connecté depuis la base")
    public ResponseEntity<byte[]> readProfilePhoto() {
        DriverProfile profile = driverService.getMyProfileEntity();

        if (profile.getProfilePhotoData() == null || profile.getProfilePhotoData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (profile.getProfilePhotoContentType() != null) {
            mediaType = MediaType.parseMediaType(profile.getProfilePhotoContentType());
        }

        String fileName = profile.getProfilePhotoFileName() != null
                ? profile.getProfilePhotoFileName()
                : "driver-profile";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(profile.getProfilePhotoData());
    }

    @PatchMapping("/online")
    @Operation(summary = "Passer en ligne")
    public ResponseEntity<ApiResponse<Void>> goOnline() {
        driverService.setOnlineStatus(true);
        return ResponseEntity.ok(ApiResponse.ok("Statut : EN LIGNE", null));
    }

    @PatchMapping("/offline")
    @Operation(summary = "Passer hors ligne")
    public ResponseEntity<ApiResponse<Void>> goOffline() {
        driverService.setOnlineStatus(false);
        return ResponseEntity.ok(ApiResponse.ok("Statut : HORS LIGNE", null));
    }

    @PatchMapping("/location")
    @Operation(summary = "Mettre à jour la position GPS")
    public ResponseEntity<ApiResponse<Void>> updateLocation(@Valid @RequestBody DriverLocationRequest request) {
        driverService.updateLocation(request);
        return ResponseEntity.ok(ApiResponse.ok("Position mise à jour", null));
    }

    @PostMapping("/sos")
    @Operation(summary = "Déclencher une alerte SOS")
    public ResponseEntity<ApiResponse<Void>> triggerSos(@Valid @RequestBody SosAlertRequest request) {
        driverService.triggerSos(request);
        return ResponseEntity.ok(ApiResponse.ok("Alerte SOS envoyée aux équipes OVIRO", null));
    }
}