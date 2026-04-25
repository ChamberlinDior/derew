package com.oviro.dto.response;

import com.oviro.enums.AddressType;
import com.oviro.model.SavedAddress;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SavedAddressResponse {

    private UUID id;
    private String label;
    private String addressName;
    private double latitude;
    private double longitude;
    private AddressType type;
    private boolean isDefault;
    private LocalDateTime createdAt;

    public static SavedAddressResponse from(SavedAddress a) {
        return SavedAddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel())
                .addressName(a.getAddressName())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .type(a.getType())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
