package com.oviro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadEvent {
    private UUID userId;
    private UUID rideId;
    private LocalDateTime readAt;
}
