package com.oviro.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareCalculator {

    private static final BigDecimal BASE_FARE = BigDecimal.valueOf(500);       // XAF
    private static final BigDecimal RATE_PER_KM = BigDecimal.valueOf(200);     // XAF/km
    private static final BigDecimal RATE_PER_MINUTE = BigDecimal.valueOf(20);  // XAF/min
    private static final BigDecimal MINIMUM_FARE = BigDecimal.valueOf(800);

    /**
     * Calcul du tarif estimé basé sur distance et durée.
     */
    public BigDecimal calculateFare(BigDecimal distanceKm, int durationMinutes) {
        BigDecimal distanceCost = RATE_PER_KM.multiply(distanceKm);
        BigDecimal timeCost = RATE_PER_MINUTE.multiply(BigDecimal.valueOf(durationMinutes));
        BigDecimal total = BASE_FARE.add(distanceCost).add(timeCost);
        return total.max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Distance Haversine entre deux coordonnées GPS (en km).
     */
    public BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1,
                                        BigDecimal lat2, BigDecimal lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;
        return BigDecimal.valueOf(distanceKm).setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Durée estimée en minutes (vitesse moyenne 30 km/h en ville).
     */
    public int estimateDuration(BigDecimal distanceKm) {
        return (int) Math.ceil(distanceKm.doubleValue() / 30.0 * 60);
    }
}
