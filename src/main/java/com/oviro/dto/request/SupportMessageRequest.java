package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportMessageRequest {

    @NotBlank
    private String content;
}
