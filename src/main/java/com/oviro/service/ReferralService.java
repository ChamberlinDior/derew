package com.oviro.service;

import com.oviro.dto.response.ReferralResponse;
import com.oviro.enums.NotificationType;
import com.oviro.enums.TransactionType;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.Referral;
import com.oviro.model.User;
import com.oviro.repository.ReferralRepository;
import com.oviro.repository.UserRepository;
import com.oviro.repository.WalletRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private static final BigDecimal REFERRER_BONUS = BigDecimal.valueOf(2000);
    private static final BigDecimal REFERRED_BONUS = BigDecimal.valueOf(1000);

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final SecurityContextHelper securityHelper;

    @Transactional(readOnly = true)
    public ReferralResponse getMyReferralStats() {
        UUID userId = securityHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        int totalReferrals = referralRepository.countByReferrerId(userId);

        return ReferralResponse.builder()
                .myReferralCode(user.getReferralCode())
                .totalReferrals(totalReferrals)
                .totalBonusEarned(BigDecimal.valueOf(totalReferrals).multiply(REFERRER_BONUS))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ReferralResponse> getMyReferrals(Pageable pageable) {
        UUID userId = securityHelper.getCurrentUserId();
        return referralRepository.findByReferrerId(userId, pageable).map(this::mapResponse);
    }

    @Transactional
    public void processReferralOnFirstRide(UUID userId) {
        Optional<Referral> opt = referralRepository.findByReferredId(userId);
        if (opt.isEmpty() || opt.get().isBonusCredited()) return;

        Referral referral = opt.get();
        referral.setFirstRideCompleted(true);

        // Créditer le parrain
        walletService.creditDriver(referral.getReferrerId(), REFERRER_BONUS,
                "Bonus parrainage - 1ère course de votre filleul");

        // Créditer le filleul
        walletService.creditDriver(userId, REFERRED_BONUS,
                "Bonus d'inscription via code parrainage");

        referral.setBonusCredited(true);
        referral.setCreditedAt(LocalDateTime.now());
        referralRepository.save(referral);

        notificationService.sendToUser(referral.getReferrerId(), NotificationType.REFERRAL_BONUS_EARNED,
                "Bonus parrainage",
                "Votre filleul a effectué sa 1ère course ! " + REFERRER_BONUS + " FCFA crédités.",
                Map.of("amount", REFERRER_BONUS.toString()), 3600L, false);

        log.info("Bonus parrainage crédité: referrer={} referred={}", referral.getReferrerId(), userId);
    }

    @Transactional
    public Referral linkReferral(String referralCode, UUID newUserId) {
        Optional<User> referrerOpt = userRepository.findByReferralCode(referralCode);
        if (referrerOpt.isEmpty()) return null;

        User referrer = referrerOpt.get();
        if (referrer.getId().equals(newUserId)) return null;

        if (referralRepository.findByReferredId(newUserId).isPresent()) return null;

        Referral referral = Referral.builder()
                .referrerId(referrer.getId())
                .referredId(newUserId)
                .referralCode(referralCode)
                .referrerBonus(REFERRER_BONUS)
                .referredBonus(REFERRED_BONUS)
                .build();

        return referralRepository.save(referral);
    }

    private ReferralResponse mapResponse(Referral r) {
        return ReferralResponse.builder()
                .id(r.getId())
                .referrerId(r.getReferrerId())
                .referredId(r.getReferredId())
                .referralCode(r.getReferralCode())
                .referrerBonus(r.getReferrerBonus())
                .referredBonus(r.getReferredBonus())
                .bonusCredited(r.isBonusCredited())
                .creditedAt(r.getCreditedAt())
                .firstRideCompleted(r.isFirstRideCompleted())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
