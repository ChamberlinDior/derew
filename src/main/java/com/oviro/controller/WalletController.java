package com.oviro.controller;

import com.oviro.dto.request.SetPinRequest;
import com.oviro.dto.request.WalletRechargeRequest;
import com.oviro.dto.request.WalletTransferRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.TransactionResponse;
import com.oviro.dto.response.WalletResponse;
import com.oviro.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet & Paiement", description = "Gestion du portefeuille et des transactions")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Consulter son wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getMyWallet()));
    }

    @PostMapping("/recharge")
    @Operation(summary = "Recharger son wallet via Mobile Money")
    public ResponseEntity<ApiResponse<TransactionResponse>> recharge(
            @Valid @RequestBody WalletRechargeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Recharge effectuée avec succès", walletService.recharge(request)));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transférer des UT vers un autre utilisateur")
    public ResponseEntity<ApiResponse<Void>> transfer(@Valid @RequestBody WalletTransferRequest request) {
        walletService.transfer(request);
        return ResponseEntity.ok(ApiResponse.ok("Transfert effectué avec succès", null));
    }

    @PostMapping("/set-pin")
    @Operation(summary = "Définir ou modifier le PIN de transfert")
    public ResponseEntity<ApiResponse<Void>> setPin(@Valid @RequestBody SetPinRequest request) {
        walletService.setTransferPin(request);
        return ResponseEntity.ok(ApiResponse.ok("PIN défini avec succès", null));
    }

    @PostMapping("/verify-pin")
    @Operation(summary = "Vérifier le PIN de transfert")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyPin(@Valid @RequestBody SetPinRequest request) {
        boolean valid = walletService.verifyPin(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("valid", valid)));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Historique des transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getMyTransactions(pageable)));
    }
}
