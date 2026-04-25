package com.oviro.service;

import com.oviro.enums.NotificationType;
import com.oviro.enums.SubscriptionStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.SubscriptionPlan;
import com.oviro.model.User;
import com.oviro.model.UserSubscription;
import com.oviro.repository.SubscriptionPlanRepository;
import com.oviro.repository.UserRepository;
import com.oviro.repository.UserSubscriptionRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationService notificationService;

    public List<SubscriptionPlan> getActivePlans() {
        return planRepository.findByActiveTrueOrderByPriceAsc();
    }

    @Transactional
    public UserSubscription subscribe(UUID planId) {
        UUID userId = securityContextHelper.getCurrentUserId();

        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).ifPresent(s -> {
            throw new BusinessException("Vous avez déjà un abonnement actif", "ALREADY_SUBSCRIBED");
        });

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait", planId.toString()));

        if (!plan.isActive()) {
            throw new BusinessException("Ce forfait n'est plus disponible", "PLAN_INACTIVE");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(now)
                .endDate(now.plusDays(plan.getDurationDays()))
                .tripsRemaining(plan.getIncludedTrips())
                .status(SubscriptionStatus.ACTIVE)
                .build();

        sub = subscriptionRepository.save(sub);
        log.info("Abonnement {} souscrit par userId={}", plan.getName(), userId);
        return sub;
    }

    @Transactional(readOnly = true)
    public List<UserSubscription> getMySubscriptions() {
        UUID userId = securityContextHelper.getCurrentUserId();
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processExpiredSubscriptions() {
        List<UserSubscription> expired = subscriptionRepository.findExpiredActive(LocalDateTime.now());
        for (UserSubscription sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Abonnement expiré userId={}", sub.getUser().getId());
        }
    }

    @Scheduled(cron = "0 0 10 * * *")
    @Transactional(readOnly = true)
    public void notifyExpiringSubscriptions() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(3);
        List<UserSubscription> expiring = subscriptionRepository.findExpiringBetween(start, end);
        for (UserSubscription sub : expiring) {
            notificationService.sendToUser(
                    sub.getUser().getId(),
                    NotificationType.SUBSCRIPTION_EXPIRING,
                    "Abonnement bientôt expiré",
                    "Votre abonnement " + sub.getPlan().getName() + " expire le " + sub.getEndDate().toLocalDate(),
                    Map.of("subscriptionId", sub.getId().toString()),
                    86400L, false
            );
        }
    }
}
