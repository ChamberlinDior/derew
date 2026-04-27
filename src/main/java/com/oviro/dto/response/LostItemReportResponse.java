package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LostItemReportResponse {
    private UUID id;
    private UUID reporterId;
    private UUID rideId;
    private String itemDescription;
    private String itemColor;
    private String contactPhone;
    private boolean resolved;
    private String resolutionNotes;
    private LocalDateTime createdAt;
}
