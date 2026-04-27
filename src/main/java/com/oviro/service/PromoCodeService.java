package com.oviro.service;

import com.oviro.dto.request.ApplyPromoCodeRequest;
import com.oviro.dto.request.PromoCodeRequest;
import com.oviro.dto.response.PromoCodeResponse;
import com.oviro.enums.PromoCodeType;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.PromoCode;
import com.oviro.repository.PromoCodeRepository;
import com.oviro.repository.PromoCodeUsageRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final SecurityContextHelper securityHelper;

    @Transactional(readOnly = true)
    public Page<PromoCodeResponse> getActivePromos(Pageable pageable) {
        return promoCodeRepository.findActivePromos(LocalDateTime.now(), pageable).map(this::mapResponse);
    }

    @Transactional(readOnly = true)
    public PromoCodeResponse applyCode(ApplyPromoCodeRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        PromoCode promo = promoCodeRepository.findByCode(request.getCode().toUpperCase())
                .orElseThrow(() -> new BusinessException("Code promo invalide", "PROMO_NOT_FOUND"));

        if (!promo.isValid()) {
            throw new BusinessException("Ce code promo est expiré ou inactif", "PROMO_EXPIRED");
        }

        int userUses = promoCodeUsageRepository.countByPromoCodeIdAndUserId(promo.getId(), userId);
        if (userUses >= promo.getUsesPerUser()) {
            throw new BusinessException("Vous avez déjà utilisé ce code promo", "PROMO_ALREADY_USED");
        }

        if (request.getRideAmount() != null && promo.getMinRideAmount() != null
                && request.getRideAmount().compareTo(promo.getMinRideAmount()) < 0) {
            throw new BusinessException(
                    "Le montant minimum pour ce code est " + promo.getMinRideAmount() + " FCFA", "PROMO_MIN_AMOUNT");
        }

        BigDecimal discount = calculateDiscount(promo, request.getRideAmount());

        PromoCodeResponse response = mapResponse(promo);
        response.setDiscountAmount(discount);
        response.setMessage("Code appliqué ! Réduction de " + discount.toPlainString() + " FCFA");
        return response;
    }

    @Transactional
    public PromoCodeResponse createCode(PromoCodeRequest request) {
        if (promoCodeRepository.findByCode(request.getCode().toUpperCase()).isPresent()) {
            throw new BusinessException("Ce code existe déjà", "PROMO_CODE_EXISTS");
        }

        PromoCode promo = PromoCode.builder()
                .code(request.getCode().toUpperCase().trim())
                .description(request.getDescription())
                .type(request.getType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minRideAmount(request.getMinRideAmount() != null ? request.getMinRideAmount() : BigDecimal.ZERO)
                .maxUses(request.getMaxUses())
                .usesPerUser(request.getUsesPerUser())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(true)
                .build();

        return mapResponse(promoCodeRepository.save(promo));
    }

    @Transactional
    public void deactivate(UUID promoId) {
        PromoCode promo = promoCodeRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("PromoCode", promoId.toString()));
        promo.setActive(false);
        promoCodeRepository.save(promo);
    }

    public BigDecimal calculateDiscount(PromoCode promo, BigDecimal rideAmount) {
        if (promo.getType() == PromoCodeType.FREE_RIDE) {
            return rideAmount != null ? rideAmount : BigDecimal.ZERO;
        }
        if (promo.getType() == PromoCodeType.FIXED_AMOUNT) {
            return promo.getDiscountValue();
        }
        // PERCENTAGE
        if (rideAmount == null) return BigDecimal.ZERO;
        BigDecimal disc = rideAmount.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (promo.getMaxDiscountAmount() != null && disc.compareTo(promo.getMaxDiscountAmount()) > 0) {
            disc = promo.getMaxDiscountAmount();
        }
        return disc;
    }

    private PromoCodeResponse mapResponse(PromoCode p) {
        return PromoCodeResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .type(p.getType())
                .discountValue(p.getDiscountValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minRideAmount(p.getMinRideAmount())
                .maxUses(p.getMaxUses())
                .usesPerUser(p.getUsesPerUser())
                .totalUsed(p.getTotalUsed())
                .validFrom(p.getValidFrom())
                .validUntil(p.getValidUntil())
                .active(p.isValid())
                .build();
    }
}
