package com.oviro.dto.response;

import com.oviro.enums.OrderStatus;
import com.oviro.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FoodOrderResponse {
    private UUID id;
    private String reference;
    private UUID clientId;
    private UUID restaurantId;
    private String restaurantName;
    private String deliveryAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
    private List<ItemResponse> items;
    private BigDecimal itemsTotal;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private String specialInstructions;
    private Integer estimatedDeliveryMinutes;
    private LocalDateTime confirmedAt;
    private LocalDateTime readyAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ItemResponse {
        private UUID menuItemId;
        private String itemName;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal subtotal;
        private String note;
    }
}
