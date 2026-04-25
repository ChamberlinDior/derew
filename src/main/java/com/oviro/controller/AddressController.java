package com.oviro.controller;

import com.oviro.dto.request.SavedAddressRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.SavedAddressResponse;
import com.oviro.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Adresses sauvegardées")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Mes adresses sauvegardées")
    public ResponseEntity<ApiResponse<List<SavedAddressResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getMyAddresses()));
    }

    @PostMapping
    @Operation(summary = "Ajouter une adresse")
    public ResponseEntity<ApiResponse<SavedAddressResponse>> create(@Valid @RequestBody SavedAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Adresse ajoutée", addressService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une adresse")
    public ResponseEntity<ApiResponse<SavedAddressResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SavedAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Adresse mise à jour", addressService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une adresse")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        addressService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Adresse supprimée", null));
    }

    @PutMapping("/{id}/set-default")
    @Operation(summary = "Définir comme adresse par défaut")
    public ResponseEntity<ApiResponse<SavedAddressResponse>> setDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Adresse par défaut mise à jour", addressService.setDefault(id)));
    }
}
