package com.oviro.controller;

import com.oviro.dto.request.FoodOrderRequest;
import com.oviro.dto.request.MenuItemRequest;
import com.oviro.dto.request.RestaurantRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.FoodOrderResponse;
import com.oviro.dto.response.MenuItemResponse;
import com.oviro.dto.response.RestaurantResponse;
import com.oviro.enums.OrderStatus;
import com.oviro.service.FoodService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
@Tag(name = "Food Delivery", description = "Commande de repas et livraison depuis les restaurants")
@SecurityRequirement(name = "bearerAuth")
public class FoodController {

    private final FoodService foodService;

    // ─── Restaurants ──────────────────────────────────────────────

    @GetMapping("/restaurants/nearby")
    @Operation(summary = "Restaurants à proximité")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> nearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.getNearbyRestaurants(lat, lng)));
    }

    @GetMapping("/restaurants")
    @Operation(summary = "Tous les restaurants actifs (paginé)")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> allRestaurants(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.getAllRestaurants(pageable)));
    }

    @GetMapping("/restaurants/{restaurantId}")
    @Operation(summary = "Détails d'un restaurant + menu")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.getRestaurant(restaurantId)));
    }

    @PostMapping("/restaurants")
    @PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
    @Operation(summary = "Créer un restaurant [Partenaire/Admin]")
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Restaurant créé", foodService.createRestaurant(request)));
    }

    // ─── Menu Items ───────────────────────────────────────────────

    @PostMapping("/restaurants/{restaurantId}/menu")
    @PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
    @Operation(summary = "Ajouter un plat au menu")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plat ajouté", foodService.addMenuItem(restaurantId, request)));
    }

    @PutMapping("/menu/{itemId}")
    @PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
    @Operation(summary = "Modifier un plat")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Plat modifié", foodService.updateMenuItem(itemId, request)));
    }

    @DeleteMapping("/menu/{itemId}")
    @PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
    @Operation(summary = "Supprimer un plat")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable UUID itemId) {
        foodService.deleteMenuItem(itemId);
        return ResponseEntity.ok(ApiResponse.ok("Plat supprimé", null));
    }

    // ─── Commandes ────────────────────────────────────────────────

    @PostMapping("/orders")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Passer une commande")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> placeOrder(
            @Valid @RequestBody FoodOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Commande passée", foodService.placeOrder(request)));
    }

    @GetMapping("/orders/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Mes commandes")
    public ResponseEntity<ApiResponse<Page<FoodOrderResponse>>> myOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.getMyOrders(pageable)));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Détails d'une commande")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.ok(foodService.getOrder(orderId)));
    }

    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('PARTNER', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Mettre à jour le statut d'une commande")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> updateStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Statut mis à jour", foodService.updateOrderStatus(orderId, status)));
    }
}
