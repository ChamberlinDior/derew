package com.oviro.dto.request;

import com.oviro.enums.OtpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtpRequest {

    @NotBlank
    private String phoneNumber;

    @NotNull
    private OtpType type;
}
