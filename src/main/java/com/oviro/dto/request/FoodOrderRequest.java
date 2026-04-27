package com.oviro.dto.request;

import com.oviro.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class FoodOrderRequest {

    @NotNull
    private UUID restaurantId;

    @NotBlank
    private String deliveryAddress;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal deliveryLatitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal deliveryLongitude;

    @NotEmpty
    private List<OrderItemRequest> items;

    @Size(max = 500)
    private String specialInstructions;

    private PaymentMethod paymentMethod = PaymentMethod.WALLET;

    @Size(max = 30)
    private String promoCode;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private UUID menuItemId;

        @Min(1) @Max(20)
        private int quantity = 1;

        @Size(max = 200)
        private String note;
    }
}
