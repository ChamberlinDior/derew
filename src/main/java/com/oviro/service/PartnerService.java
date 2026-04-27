package com.oviro.service;

import com.oviro.dto.request.PartnerProfileRequest;
import com.oviro.dto.response.PartnerProfileResponse;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.PartnerProfile;
import com.oviro.model.User;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.PartnerProfileRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerService {

    private final PartnerProfileRepository partnerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityHelper;

    @Transactional(readOnly = true)
    public PartnerProfileResponse getMyProfile() {
        UUID userId = securityHelper.getCurrentUserId();
        PartnerProfile profile = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil partenaire", userId.toString()));
        return mapToResponse(profile);
    }

    @Transactional
    public PartnerProfileResponse updateMyProfile(PartnerProfileRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        PartnerProfile profile = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil partenaire", userId.toString()));

        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            profile.setCompanyName(request.getCompanyName().trim());
        }
        if (request.getCompanyRegistrationNumber() != null) {
            if (!request.getCompanyRegistrationNumber().equals(profile.getCompanyRegistrationNumber())
                    && partnerProfileRepository.existsByCompanyRegistrationNumber(request.getCompanyRegistrationNumber())) {
                throw new BusinessException("Ce numéro d'enregistrement est déjà utilisé");
            }
            profile.setCompanyRegistrationNumber(request.getCompanyRegistrationNumber().trim());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress().trim());
        }

        profile = partnerProfileRepository.save(profile);
        log.info("Profil partenaire mis à jour pour userId={}", userId);
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<DriverSummary> getMyDrivers() {
        UUID userId = securityHelper.getCurrentUserId();
        PartnerProfile profile = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil partenaire", userId.toString()));
        return profile.getDrivers().stream().map(this::toDriverSummary).toList();
    }

    @Transactional
    public void assignDriverToPartner(UUID driverProfileId) {
        UUID userId = securityHelper.getCurrentUserId();
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil partenaire", userId.toString()));

        DriverProfile driver = driverProfileRepository.findById(driverProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur", driverProfileId.toString()));

        driver.setPartner(partner);
        driverProfileRepository.save(driver);
        log.info("Chauffeur {} assigné au partenaire {}", driverProfileId, partner.getId());
    }

    @Transactional
    public void removeDriverFromPartner(UUID driverProfileId) {
        UUID userId = securityHelper.getCurrentUserId();
        PartnerProfile partner = partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil partenaire", userId.toString()));

        DriverProfile driver = driverProfileRepository.findById(driverProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur", driverProfileId.toString()));

        if (driver.getPartner() == null || !driver.getPartner().getId().equals(partner.getId())) {
            throw new BusinessException("Ce chauffeur n'appartient pas à votre flotte");
        }

        driver.setPartner(null);
        driverProfileRepository.save(driver);
    }

    // --- Admin methods ---

    @Transactional(readOnly = true)
    public Page<PartnerProfileResponse> getAllPartners(Pageable pageable) {
        return partnerProfileRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public PartnerProfileResponse verifyPartner(UUID partnerId, boolean verified) {
        PartnerProfile profile = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partenaire", partnerId.toString()));
        profile.setVerified(verified);
        profile = partnerProfileRepository.save(profile);
        log.info("Partenaire {} {} par l'admin", partnerId, verified ? "vérifié" : "non vérifié");
        return mapToResponse(profile);
    }

    @Transactional
    public PartnerProfileResponse updateCommissionRate(UUID partnerId, BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("Le taux de commission doit être entre 0 et 1");
        }
        PartnerProfile profile = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partenaire", partnerId.toString()));
        profile.setCommissionRate(rate);
        profile = partnerProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    private DriverSummary toDriverSummary(DriverProfile d) {
        User u = d.getUser();
        return DriverSummary.builder()
                .id(d.getId())
                .userId(u.getId())
                .fullName(u.getFullName())
                .phoneNumber(u.getPhoneNumber())
                .licenseNumber(d.getLicenseNumber())
                .status(d.getStatus().name())
                .rating(d.getRating())
                .totalRides(d.getTotalRides())
                .build();
    }

    @Data
    @Builder
    public static class DriverSummary {
        private UUID id;
        private UUID userId;
        private String fullName;
        private String phoneNumber;
        private String licenseNumber;
        private String status;
        private java.math.BigDecimal rating;
        private int totalRides;
    }

    private PartnerProfileResponse mapToResponse(PartnerProfile p) {
        User user = p.getUser();
        return PartnerProfileResponse.builder()
                .id(p.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .companyName(p.getCompanyName())
                .companyRegistrationNumber(p.getCompanyRegistrationNumber())
                .address(p.getAddress())
                .commissionRate(p.getCommissionRate())
                .verified(p.isVerified())
                .driverCount(p.getDrivers() != null ? p.getDrivers().size() : 0)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
