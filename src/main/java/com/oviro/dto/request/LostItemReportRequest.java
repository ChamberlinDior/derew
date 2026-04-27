package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class LostItemReportRequest {

    @NotNull
    private UUID rideId;

    @NotBlank
    @Size(max = 500)
    private String itemDescription;

    @Size(max = 100)
    private String itemColor;

    @Size(max = 20)
    private String contactPhone;
}
