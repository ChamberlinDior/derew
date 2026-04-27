package com.oviro.dto.request;

import com.oviro.enums.SupportTicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SupportTicketRequest {

    @NotNull
    private SupportTicketCategory category;

    @NotBlank
    @Size(max = 200)
    private String subject;

    @NotBlank
    private String firstMessage;

    private UUID relatedRideId;
    private UUID relatedDeliveryId;
    private UUID relatedOrderId;
}
