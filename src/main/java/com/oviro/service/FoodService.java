package com.oviro.service;

import com.oviro.dto.request.FoodOrderRequest;
import com.oviro.dto.request.MenuItemRequest;
import com.oviro.dto.request.RestaurantRequest;
import com.oviro.dto.response.FoodOrderResponse;
import com.oviro.dto.response.MenuItemResponse;
import com.oviro.dto.response.RestaurantResponse;
import com.oviro.enums.NotificationType;
import com.oviro.enums.OrderStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.FoodOrder;
import com.oviro.model.FoodOrderItem;
import com.oviro.model.MenuItem;
import com.oviro.model.RestaurantProfile;
import com.oviro.repository.FoodOrderRepository;
import com.oviro.repository.MenuItemRepository;
import com.oviro.repository.RestaurantProfileRepository;
import com.oviro.util.ReferenceGenerator;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodService {

    private final RestaurantProfileRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityHelper;
    private final NotificationService notificationService;
    private final PromoCodeService promoCodeService;

    // ─── Restaurants ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getNearbyRestaurants(BigDecimal lat, BigDecimal lng) {
        return restaurantRepository.findNearby(lat, lng).stream()
                .map(r -> mapRestaurant(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getAllRestaurants(Pageable pageable) {
        return restaurantRepository.findByActiveTrueAndVerifiedTrue(pageable).map(r -> mapRestaurant(r, null));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(UUID restaurantId) {
        RestaurantProfile r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId.toString()));
        List<MenuItem> items = menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId);
        return mapRestaurantWithMenu(r, items);
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        UUID ownerId = securityHelper.getCurrentUserId();
        RestaurantProfile restaurant = RestaurantProfile.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .phone(request.getPhone())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .cuisineType(request.getCuisineType())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .deliveryRadiusKm(request.getDeliveryRadiusKm() != null ? request.getDeliveryRadiusKm() : BigDecimal.valueOf(5))
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.valueOf(1000))
                .deliveryFee(request.getDeliveryFee() != null ? request.getDeliveryFee() : BigDecimal.valueOf(500))
                .averagePrepMinutes(request.getAveragePrepMinutes())
                .build();
        return mapRestaurant(restaurantRepository.save(restaurant), null);
    }

    // ─── Menu Items ───────────────────────────────────────────────

    @Transactional
    public MenuItemResponse addMenuItem(UUID restaurantId, MenuItemRequest request) {
        RestaurantProfile restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId.toString()));

        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .available(request.isAvailable())
                .popular(request.isPopular())
                .preparationMinutes(request.getPreparationMinutes())
                .build();
        return mapMenuItem(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(UUID itemId, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Plat", itemId.toString()));
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setPrice(request.getPrice());
        item.setAvailable(request.isAvailable());
        item.setPopular(request.isPopular());
        return mapMenuItem(menuItemRepository.save(item));
    }

    @Transactional
    public void deleteMenuItem(UUID itemId) {
        menuItemRepository.deleteById(itemId);
    }

    // ─── Commandes ────────────────────────────────────────────────

    @Transactional
    public FoodOrderResponse placeOrder(FoodOrderRequest request) {
        UUID clientId = securityHelper.getCurrentUserId();
        RestaurantProfile restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", request.getRestaurantId().toString()));

        if (!restaurant.isActive() || !restaurant.isVerified()) {
            throw new BusinessException("Ce restaurant n'accepte pas de commandes pour le moment");
        }

        List<FoodOrderItem> orderItems = new ArrayList<>();
        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (FoodOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            MenuItem menu = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plat", itemReq.getMenuItemId().toString()));

            if (!menu.isAvailable()) {
                throw new BusinessException("Le plat '" + menu.getName() + "' n'est plus disponible");
            }
            if (!menu.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BusinessException("Ce plat n'appartient pas à ce restaurant");
            }

            BigDecimal subtotal = menu.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            itemsTotal = itemsTotal.add(subtotal);

            orderItems.add(FoodOrderItem.builder()
                    .menuItemId(menu.getId())
                    .itemName(menu.getName())
                    .unitPrice(menu.getPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .note(itemReq.getNote())
                    .build());
        }

        if (itemsTotal.compareTo(restaurant.getMinOrderAmount()) < 0) {
            throw new BusinessException("Commande minimale : " + restaurant.getMinOrderAmount() + " FCFA");
        }

        BigDecimal deliveryFee = restaurant.getDeliveryFee();
        BigDecimal discount = BigDecimal.ZERO;

        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            try {
                com.oviro.dto.request.ApplyPromoCodeRequest applyReq = new com.oviro.dto.request.ApplyPromoCodeRequest();
                applyReq.setCode(request.getPromoCode());
                applyReq.setRideAmount(itemsTotal);
                discount = promoCodeService.applyCode(applyReq).getDiscountAmount();
            } catch (BusinessException ignored) {}
        }

        BigDecimal total = itemsTotal.add(deliveryFee).subtract(discount).max(BigDecimal.ZERO);

        FoodOrder order = FoodOrder.builder()
                .reference(referenceGenerator.generateRideReference())
                .clientId(clientId)
                .restaurant(restaurant)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .itemsTotal(itemsTotal)
                .deliveryFee(deliveryFee)
                .discountAmount(discount)
                .totalAmount(total)
                .paymentMethod(request.getPaymentMethod())
                .specialInstructions(request.getSpecialInstructions())
                .estimatedDeliveryMinutes(restaurant.getAveragePrepMinutes() + 20)
                .build();

        order = foodOrderRepository.save(order);
        for (FoodOrderItem fi : orderItems) {
            fi.setOrder(order);
        }
        order.getItems().addAll(orderItems);
        order = foodOrderRepository.save(order);

        notificationService.sendToUser(clientId, NotificationType.FOOD_ORDER_CONFIRMED,
                "Commande confirmée",
                "Votre commande chez " + restaurant.getName() + " est enregistrée !",
                Map.of("orderId", order.getId().toString()), 3600L, false);

        log.info("Commande food créée: {} pour client={}", order.getReference(), clientId);
        return mapOrderResponse(order);
    }

    @Transactional
    public FoodOrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", orderId.toString()));

        order.setStatus(newStatus);
        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedAt(LocalDateTime.now());
            case READY_FOR_PICKUP -> order.setReadyAt(LocalDateTime.now());
            case PICKED_UP -> order.setPickedUpAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> order.setCancelledAt(LocalDateTime.now());
            default -> {}
        }
        order = foodOrderRepository.save(order);

        if (newStatus == OrderStatus.DELIVERED) {
            notificationService.sendToUser(order.getClientId(), NotificationType.FOOD_ORDER_DELIVERED,
                    "Commande livrée", "Votre commande de " + order.getRestaurant().getName() + " a été livrée !",
                    Map.of("orderId", orderId.toString()), 3600L, false);
        }

        return mapOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<FoodOrderResponse> getMyOrders(Pageable pageable) {
        UUID clientId = securityHelper.getCurrentUserId();
        return foodOrderRepository.findByClientId(clientId, pageable).map(this::mapOrderResponse);
    }

    @Transactional(readOnly = true)
    public FoodOrderResponse getOrder(UUID orderId) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", orderId.toString()));
        return mapOrderResponse(order);
    }

    // ─── Mapping ──────────────────────────────────────────────────

    private RestaurantResponse mapRestaurant(RestaurantProfile r, List<MenuItem> items) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .phone(r.getPhone())
                .address(r.getAddress())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .cuisineType(r.getCuisineType())
                .openingTime(r.getOpeningTime())
                .closingTime(r.getClosingTime())
                .deliveryRadiusKm(r.getDeliveryRadiusKm())
                .minOrderAmount(r.getMinOrderAmount())
                .deliveryFee(r.getDeliveryFee())
                .averagePrepMinutes(r.getAveragePrepMinutes())
                .rating(r.getRating())
                .totalOrders(r.getTotalOrders())
                .active(r.isActive())
                .verified(r.isVerified())
                .logoUrl(r.getLogoUrl())
                .menuItems(items != null ? items.stream().map(this::mapMenuItem).toList() : null)
                .build();
    }

    private RestaurantResponse mapRestaurantWithMenu(RestaurantProfile r, List<MenuItem> items) {
        RestaurantResponse resp = mapRestaurant(r, items);
        return resp;
    }

    private MenuItemResponse mapMenuItem(MenuItem m) {
        return MenuItemResponse.builder()
                .id(m.getId())
                .restaurantId(m.getRestaurant().getId())
                .name(m.getName())
                .description(m.getDescription())
                .category(m.getCategory())
                .price(m.getPrice())
                .photoUrl(m.getPhotoUrl())
                .available(m.isAvailable())
                .popular(m.isPopular())
                .preparationMinutes(m.getPreparationMinutes())
                .build();
    }

    private FoodOrderResponse mapOrderResponse(FoodOrder o) {
        return FoodOrderResponse.builder()
                .id(o.getId())
                .reference(o.getReference())
                .clientId(o.getClientId())
                .restaurantId(o.getRestaurant().getId())
                .restaurantName(o.getRestaurant().getName())
                .deliveryAddress(o.getDeliveryAddress())
                .deliveryLatitude(o.getDeliveryLatitude())
                .deliveryLongitude(o.getDeliveryLongitude())
                .items(o.getItems().stream().map(i -> FoodOrderResponse.ItemResponse.builder()
                        .menuItemId(i.getMenuItemId())
                        .itemName(i.getItemName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getSubtotal())
                        .note(i.getNote())
                        .build()).toList())
                .itemsTotal(o.getItemsTotal())
                .deliveryFee(o.getDeliveryFee())
                .discountAmount(o.getDiscountAmount())
                .totalAmount(o.getTotalAmount())
                .paymentMethod(o.getPaymentMethod())
                .status(o.getStatus())
                .specialInstructions(o.getSpecialInstructions())
                .estimatedDeliveryMinutes(o.getEstimatedDeliveryMinutes())
                .confirmedAt(o.getConfirmedAt())
                .readyAt(o.getReadyAt())
                .deliveredAt(o.getDeliveredAt())
                .cancelledAt(o.getCancelledAt())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
