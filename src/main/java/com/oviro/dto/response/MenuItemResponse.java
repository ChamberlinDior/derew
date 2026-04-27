package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MenuItemResponse {
    private UUID id;
    private UUID restaurantId;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String photoUrl;
    private boolean available;
    private boolean popular;
    private int preparationMinutes;
}
