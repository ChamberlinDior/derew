package com.oviro.dto.request;

import com.oviro.enums.PaymentMethod;
import com.oviro.enums.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RideRequest {

    @NotBlank(message = "L'adresse de prise en charge est obligatoire")
    private String pickupAddress;

    @NotNull(message = "La latitude de prise en charge est obligatoire")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal pickupLatitude;

    @NotNull(message = "La longitude de prise en charge est obligatoire")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal pickupLongitude;

    @NotBlank(message = "L'adresse de destination est obligatoire")
    private String dropoffAddress;

    @NotNull(message = "La latitude de destination est obligatoire")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal dropoffLatitude;

    @NotNull(message = "La longitude de destination est obligatoire")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal dropoffLongitude;

    private ServiceType serviceType = ServiceType.STANDARD;

    // Mode de paiement
    private PaymentMethod paymentMethod = PaymentMethod.WALLET;

    // Code promo
    @Size(max = 30)
    private String promoCode;

    // Course planifiée
    private LocalDateTime scheduledAt;

    // Multi-arrêts (arrêts intermédiaires)
    private List<StopRequest> stops;

    // Adresse sauvegardée (alternative aux coordonnées)
    private UUID savedPickupAddressId;
    private UUID savedDropoffAddressId;

    // Pour quelqu'un d'autre
    private boolean forSomeoneElse = false;

    @Size(max = 20)
    private String recipientPhone;

    @Size(max = 200)
    private String recipientName;

    @Size(max = 500)
    private String senderNote;

    @Data
    public static class StopRequest {
        @NotBlank
        private String address;

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        private BigDecimal latitude;

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        private BigDecimal longitude;

        @Size(max = 200)
        private String note;
    }
}
