package com.oviro.service;

import com.oviro.dto.request.OtpRequest;
import com.oviro.dto.request.VerifyOtpRequest;
import com.oviro.enums.OtpType;
import com.oviro.enums.UserStatus;
import com.oviro.exception.BusinessException;
import com.oviro.model.User;
import com.oviro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp_attempts:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public Map<String, String> sendOtp(OtpRequest request) {
        String phone = request.getPhoneNumber().trim();
        String otpType = request.getType().name();

        String attemptsKey = ATTEMPTS_KEY_PREFIX + phone + ":" + otpType;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        if (attemptsStr != null && Integer.parseInt(attemptsStr) >= MAX_ATTEMPTS) {
            throw new BusinessException("Trop de tentatives. Réessayez dans 30 minutes.", "OTP_TOO_MANY_ATTEMPTS");
        }

        String code = generateCode();
        String redisKey = OTP_KEY_PREFIX + phone + ":" + otpType;

        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(OTP_TTL_MINUTES));
        log.info("OTP généré pour {} [{}]: {} (simulé — intégrer SMS provider)", phone, otpType, code);

        // En production: appel API Twilio / Africa's Talking / etc.
        // smsProvider.send(phone, "Votre code OVIRO : " + code);

        return Map.of(
                "message", "Code envoyé au " + phone,
                "expiresInMinutes", String.valueOf(OTP_TTL_MINUTES),
                // À retirer en production !
                "code_debug", code
        );
    }

    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        String phone = request.getPhoneNumber().trim();
        String otpType = request.getType().name();
        String redisKey = OTP_KEY_PREFIX + phone + ":" + otpType;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + phone + ":" + otpType;

        String stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            throw new BusinessException("Code OTP expiré ou invalide. Demandez un nouveau code.", "OTP_EXPIRED");
        }

        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (!stored.equals(request.getCode().trim())) {
            attempts++;
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), Duration.ofMinutes(30));
            throw new BusinessException("Code OTP incorrect. " + (MAX_ATTEMPTS - attempts) + " tentative(s) restante(s).", "OTP_INVALID");
        }

        // OTP valide — nettoyer
        redisTemplate.delete(redisKey);
        redisTemplate.delete(attemptsKey);

        // Marquer téléphone vérifié si c'est une vérification de compte
        if (request.getType() == OtpType.PHONE_VERIFICATION) {
            userRepository.findByPhoneNumber(phone).ifPresent(user -> {
                user.setPhoneVerified(true);
                if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                    user.setStatus(UserStatus.ACTIVE);
                }
                userRepository.save(user);
                log.info("Téléphone vérifié pour userId={}", user.getId());
            });
        }

        return true;
    }

    private String generateCode() {
        SecureRandom rng = new SecureRandom();
        int code = 100000 + rng.nextInt(900000);
        return String.valueOf(code);
    }
}
