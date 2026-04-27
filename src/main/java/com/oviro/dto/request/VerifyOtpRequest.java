package com.oviro.dto.request;

import com.oviro.enums.OtpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    private String phoneNumber;

    @NotBlank
    @Size(min = 4, max = 6)
    private String code;

    @NotNull
    private OtpType type;
}
