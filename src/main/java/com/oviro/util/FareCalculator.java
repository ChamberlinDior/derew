package com.oviro.util;

import com.oviro.enums.ServiceType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Component
public class FareCalculator {

    private static final BigDecimal BASE_FARE        = BigDecimal.valueOf(500);
    private static final BigDecimal RATE_PER_KM      = BigDecimal.valueOf(200);
    private static final BigDecimal RATE_PER_MINUTE  = BigDecimal.valueOf(20);
    private static final BigDecimal MINIMUM_FARE     = BigDecimal.valueOf(800);

    private static final BigDecimal NIGHT_SURCHARGE       = BigDecimal.valueOf(1.3);
    private static final BigDecimal PEAK_HOUR_SURCHARGE   = BigDecimal.valueOf(1.2);

    public BigDecimal calculateFare(BigDecimal distanceKm, int durationMinutes) {
        return calculateFare(distanceKm, durationMinutes, ServiceType.STANDARD);
    }

    public BigDecimal calculateFare(BigDecimal distanceKm, int durationMinutes, ServiceType serviceType) {
        BigDecimal distanceCost = RATE_PER_KM.multiply(distanceKm);
        BigDecimal timeCost = RATE_PER_MINUTE.multiply(BigDecimal.valueOf(durationMinutes));
        BigDecimal base = BASE_FARE.add(distanceCost).add(timeCost);

        // Appliquer le multiplicateur selon le type de service
        double multiplier = serviceType != null ? serviceType.getPriceMultiplier() : 1.0;
        BigDecimal fare = base.multiply(BigDecimal.valueOf(multiplier));

        // Surcharge heures de pointe (7h-9h, 17h-19h)
        if (isPeakHour()) {
            fare = fare.multiply(PEAK_HOUR_SURCHARGE);
        }

        // Surcharge nuit (22h-6h)
        if (isNightTime()) {
            fare = fare.multiply(NIGHT_SURCHARGE);
        }

        return fare.max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);
    }

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

    public int estimateDuration(BigDecimal distanceKm) {
        return (int) Math.ceil(distanceKm.doubleValue() / 30.0 * 60);
    }

    private boolean isPeakHour() {
        LocalTime now = LocalTime.now();
        return (now.isAfter(LocalTime.of(7, 0)) && now.isBefore(LocalTime.of(9, 0)))
                || (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(19, 0)));
    }

    private boolean isNightTime() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(6, 0));
    }
}
