package com.oviro.repository;

import com.oviro.enums.OrderStatus;
import com.oviro.model.FoodOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, UUID> {

    Page<FoodOrder> findByClientId(UUID clientId, Pageable pageable);

    Page<FoodOrder> findByRestaurantIdAndStatusNot(UUID restaurantId, OrderStatus status, Pageable pageable);

    Page<FoodOrder> findByDriverId(UUID driverId, Pageable pageable);

    Optional<FoodOrder> findByReference(String reference);

    boolean existsByReference(String reference);
}
