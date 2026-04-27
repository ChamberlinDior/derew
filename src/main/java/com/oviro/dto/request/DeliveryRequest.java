package com.oviro.dto.request;

import com.oviro.enums.PackageType;
import com.oviro.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryRequest {

    @NotBlank
    private String pickupAddress;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal pickupLatitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal pickupLongitude;

    @NotBlank
    private String dropoffAddress;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal dropoffLatitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal dropoffLongitude;

    @NotBlank
    @Size(max = 200)
    private String recipientName;

    @NotBlank
    @Size(max = 20)
    private String recipientPhone;

    private PackageType packageType = PackageType.OTHER;

    @Size(max = 500)
    private String packageDescription;

    @DecimalMin("0.0")
    private BigDecimal packageWeightKg;

    private boolean fragile = false;

    @Size(max = 500)
    private String senderNote;

    private PaymentMethod paymentMethod = PaymentMethod.WALLET;

    @Size(max = 30)
    private String promoCode;
}
