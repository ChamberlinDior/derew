package com.oviro.controller;

import com.oviro.dto.response.ApiResponse;
import com.oviro.model.SubscriptionPlan;
import com.oviro.model.UserSubscription;
import com.oviro.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Abonnements")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    @Operation(summary = "Forfaits disponibles")
    public ResponseEntity<ApiResponse<List<SubscriptionPlan>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.getActivePlans()));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Souscrire un forfait")
    public ResponseEntity<ApiResponse<UserSubscription>> subscribe(@RequestBody Map<String, String> body) {
        UUID planId = UUID.fromString(body.get("planId"));
        return ResponseEntity.ok(ApiResponse.ok("Abonnement activé", subscriptionService.subscribe(planId)));
    }

    @GetMapping("/my")
    @Operation(summary = "Mes abonnements")
    public ResponseEntity<ApiResponse<List<UserSubscription>>> getMy() {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.getMySubscriptions()));
    }
}
