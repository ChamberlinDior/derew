package com.oviro.service;

import com.oviro.dto.response.PricingZoneResponse;
import com.oviro.dto.response.SurgeInfoResponse;
import com.oviro.model.PricingZone;
import com.oviro.repository.PricingZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurgeService {

    private final PricingZoneRepository pricingZoneRepository;

    @Transactional(readOnly = true)
    public SurgeInfoResponse getSurgeInfo(BigDecimal lat, BigDecimal lng) {
        Optional<PricingZone> zone = pricingZoneRepository.findZoneContaining(lat, lng);
        if (zone.isEmpty() || zone.get().getSurgeMultiplier().compareTo(BigDecimal.ONE) <= 0) {
            return SurgeInfoResponse.builder()
                    .surgeActive(false)
                    .surgeMultiplier(BigDecimal.ONE)
                    .message("Tarif normal")
                    .build();
        }
        PricingZone z = zone.get();
        return SurgeInfoResponse.builder()
                .surgeActive(true)
                .surgeMultiplier(z.getSurgeMultiplier())
                .zoneName(z.getName())
                .message("Forte demande dans votre zone. Multiplicateur x" + z.getSurgeMultiplier())
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getSurgeMultiplier(BigDecimal lat, BigDecimal lng) {
        return pricingZoneRepository.findZoneContaining(lat, lng)
                .map(PricingZone::getSurgeMultiplier)
                .orElse(BigDecimal.ONE);
    }

    @Transactional(readOnly = true)
    public List<PricingZoneResponse> getAllZones() {
        return pricingZoneRepository.findAll().stream().map(this::mapZone).toList();
    }

    @Transactional
    public PricingZoneResponse createZone(com.oviro.dto.request.PricingZoneRequest request) {
        PricingZone zone = PricingZone.builder()
                .name(request.getName())
                .description(request.getDescription())
                .centerLatitude(request.getCenterLatitude())
                .centerLongitude(request.getCenterLongitude())
                .radiusKm(request.getRadiusKm())
                .surgeMultiplier(request.getSurgeMultiplier())
                .baseFareOverride(request.getBaseFareOverride())
                .active(request.isActive())
                .build();
        return mapZone(pricingZoneRepository.save(zone));
    }

    @Transactional
    public PricingZoneResponse updateZone(java.util.UUID zoneId, com.oviro.dto.request.PricingZoneRequest req) {
        PricingZone zone = pricingZoneRepository.findById(zoneId)
                .orElseThrow(() -> new com.oviro.exception.ResourceNotFoundException("Zone", zoneId.toString()));
        zone.setName(req.getName());
        if (req.getDescription() != null) zone.setDescription(req.getDescription());
        zone.setCenterLatitude(req.getCenterLatitude());
        zone.setCenterLongitude(req.getCenterLongitude());
        zone.setRadiusKm(req.getRadiusKm());
        zone.setSurgeMultiplier(req.getSurgeMultiplier());
        zone.setBaseFareOverride(req.getBaseFareOverride());
        zone.setActive(req.isActive());
        return mapZone(pricingZoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(java.util.UUID zoneId) {
        pricingZoneRepository.deleteById(zoneId);
    }

    private PricingZoneResponse mapZone(PricingZone z) {
        return PricingZoneResponse.builder()
                .id(z.getId())
                .name(z.getName())
                .description(z.getDescription())
                .centerLatitude(z.getCenterLatitude())
                .centerLongitude(z.getCenterLongitude())
                .radiusKm(z.getRadiusKm())
                .surgeMultiplier(z.getSurgeMultiplier())
                .baseFareOverride(z.getBaseFareOverride())
                .active(z.isActive())
                .build();
    }
}
