package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RestaurantResponse {
    private UUID id;
    private String name;
    private String description;
    private String phone;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String cuisineType;
    private String openingTime;
    private String closingTime;
    private BigDecimal deliveryRadiusKm;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private int averagePrepMinutes;
    private BigDecimal rating;
    private int totalOrders;
    private boolean active;
    private boolean verified;
    private String logoUrl;
    private List<MenuItemResponse> menuItems;
    private Double distanceKm;
}
