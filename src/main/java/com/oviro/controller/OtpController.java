package com.oviro.controller;

import com.oviro.dto.request.OtpRequest;
import com.oviro.dto.request.VerifyOtpRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(name = "OTP / Vérification", description = "Envoi et vérification de codes OTP par SMS")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    @Operation(summary = "Envoyer un code OTP par SMS")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@Valid @RequestBody OtpRequest request) {
        Map<String, String> result = otpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP envoyé", result));
    }

    @PostMapping("/verify")
    @Operation(summary = "Vérifier un code OTP")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean verified = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Vérification réussie", Map.of("verified", verified)));
    }
}
