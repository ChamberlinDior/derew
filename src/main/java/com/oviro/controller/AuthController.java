package com.oviro.controller;

import com.oviro.dto.request.LoginRequest;
import com.oviro.dto.request.RefreshTokenRequest;
import com.oviro.dto.request.RegisterRequest;
import com.oviro.dto.request.UpdateUserProfileRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.AuthResponse;
import com.oviro.dto.response.UploadUserProfilePhotoResponse;
import com.oviro.dto.response.UserProfilePhotoData;
import com.oviro.dto.response.UserResponse;
import com.oviro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, refresh token, déconnexion")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Inscription réussie", user));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion – retourne access + refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse auth = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Connexion réussie", auth));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token d'accès via refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse auth = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token renouvelé", auth));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion – révocation de la session courante")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok("Déconnexion réussie", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        UserResponse user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Profil récupéré avec succès", user));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Mettre à jour le profil du rider connecté")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UUID userId = extractUserId(authentication);
        UserResponse user = authService.updateCurrentUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profil mis à jour avec succès", user));
    }

    @PostMapping(value = "/me/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter ou remplacer la photo de profil du rider connecté")
    public ResponseEntity<ApiResponse<UploadUserProfilePhotoResponse>> uploadCurrentUserProfilePhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        UUID userId = extractUserId(authentication);
        UploadUserProfilePhotoResponse response = authService.uploadCurrentUserProfilePhoto(userId, file);
        return ResponseEntity.ok(ApiResponse.ok("Photo de profil mise à jour avec succès", response));
    }

    @GetMapping("/me/profile-photo")
    @Operation(summary = "Récupérer la photo de profil du rider connecté")
    public ResponseEntity<ByteArrayResource> getCurrentUserProfilePhoto(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        UserProfilePhotoData photo = authService.getCurrentUserProfilePhoto(userId);

        ByteArrayResource resource = new ByteArrayResource(photo.getData());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(photo.getContentType()));
        headers.setContentLength(photo.getSize());
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(photo.getFileName(), StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID uuid) {
            return uuid;
        }

        if (principal instanceof String value && !value.isBlank()) {
            return UUID.fromString(value);
        }

        throw new IllegalStateException("Principal JWT invalide");
    }
}