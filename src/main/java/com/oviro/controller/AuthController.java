package com.oviro.controller;

import com.oviro.dto.request.LoginRequest;
import com.oviro.dto.request.RefreshTokenRequest;
import com.oviro.dto.request.RegisterRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.AuthResponse;
import com.oviro.dto.response.UserResponse;
import com.oviro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            HttpServletRequest httpRequest) {
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
}
