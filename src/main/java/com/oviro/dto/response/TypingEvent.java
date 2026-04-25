package com.oviro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {
    private UUID userId;
    private String userName;
    private boolean typing;
}
