package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items", indexes = {
    @Index(name = "idx_menu_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_menu_available", columnList = "available")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantProfile restaurant;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "available")
    @Builder.Default
    private boolean available = true;

    @Column(name = "is_popular")
    @Builder.Default
    private boolean popular = false;

    @Column(name = "preparation_minutes")
    @Builder.Default
    private int preparationMinutes = 10;
}
