package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FCMTokenRequest {

    @NotBlank(message = "Le token FCM est obligatoire")
    private String fcmToken;
}
