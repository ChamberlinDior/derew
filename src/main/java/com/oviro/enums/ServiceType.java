package com.oviro.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceType {
    STANDARD(1.0),
    CONFORT(1.3),
    LUXE(2.0),
    MOTO(0.7),
    MINIBUS(1.5),
    LOCATION(2.0),
    FEMME_ONLY(1.1),
    ANIMAL_OK(1.2),
    BAGAGE_PLUS(1.3);

    private final double priceMultiplier;
}
