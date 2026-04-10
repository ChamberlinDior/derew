package com.oviro.service;

import com.oviro.dto.request.LoginRequest;
import com.oviro.dto.request.RefreshTokenRequest;
import com.oviro.dto.request.RegisterRequest;
import com.oviro.dto.response.AuthResponse;
import com.oviro.dto.response.UserResponse;
import com.oviro.enums.Role;
import com.oviro.enums.UserStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.UnauthorizedException;
import com.oviro.model.SessionToken;
import com.oviro.model.User;
import com.oviro.model.Wallet;
import com.oviro.repository.SessionTokenRepository;
import com.oviro.repository.UserRepository;
import com.oviro.repository.WalletRepository;
import com.oviro.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé", "PHONE_ALREADY_EXISTS");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Cet email est déjà utilisé", "EMAIL_ALREADY_EXISTS");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        // Création automatique du wallet
        Wallet wallet = Wallet.builder().user(user).build();
        walletRepository.save(wallet);

        log.info("Nouvel utilisateur enregistré: {} [{}]", user.getFullName(), user.getRole());
        return mapToResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByPhoneNumber(request.getIdentifier()))
                .orElseThrow(() -> new UnauthorizedException("Identifiants incorrects"));

        if (user.isAccountLocked()) {
            throw new BusinessException("Compte temporairement verrouillé. Réessayez plus tard.", "ACCOUNT_LOCKED");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Compte inactif ou suspendu", "ACCOUNT_INACTIVE");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Identifiants incorrects");
        }

        // Reset tentatives
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Persister la session
        SessionToken session = SessionToken.builder()
                .user(user)
                .token(accessToken)
                .deviceInfo(request.getDeviceInfo())
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusSeconds(900)) // 15 min
                .build();
        sessionTokenRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(mapToResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtUtil.validateToken(token) || !"REFRESH".equals(jwtUtil.extractTokenType(token))) {
            throw new UnauthorizedException("Refresh token invalide ou expiré");
        }

        var userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name(), user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        SessionToken session = SessionToken.builder()
                .user(user)
                .token(newAccessToken)
                .expiresAt(LocalDateTime.now().plusSeconds(900))
                .build();
        sessionTokenRepository.save(session);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(mapToResponse(user))
                .build();
    }

    @Transactional
    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            sessionTokenRepository.revokeByToken(token);
        }
    }

    @Transactional
    public void revokeAllSessions(java.util.UUID userId) {
        sessionTokenRepository.revokeAllUserSessions(userId);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Compte verrouillé après {} tentatives: {}", attempts, user.getPhoneNumber());
        }
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
