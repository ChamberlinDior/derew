package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String category;

    @NotNull
    @DecimalMin("1.0")
    private BigDecimal price;

    private boolean available = true;
    private boolean popular = false;

    @Min(1) @Max(120)
    private int preparationMinutes = 10;
}
