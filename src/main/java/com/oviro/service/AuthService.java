package com.oviro.service;

import com.oviro.dto.request.LoginRequest;
import com.oviro.dto.request.RefreshTokenRequest;
import com.oviro.dto.request.RegisterRequest;
import com.oviro.dto.request.UpdateUserProfileRequest;
import com.oviro.dto.response.AuthResponse;
import com.oviro.dto.response.UploadUserProfilePhotoResponse;
import com.oviro.dto.response.UserProfilePhotoData;
import com.oviro.dto.response.UserResponse;
import com.oviro.enums.DriverStatus;
import com.oviro.enums.Role;
import com.oviro.enums.UserStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.UnauthorizedException;
import com.oviro.model.DriverProfile;
import com.oviro.model.SessionToken;
import com.oviro.model.User;
import com.oviro.model.Wallet;
import com.oviro.repository.DriverProfileRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;
    private final ReferralService referralService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        validateUniqueEmailAndPhoneForCreate(request.getEmail(), request.getPhoneNumber());

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizeNullable(request.getEmail()))
                .phoneNumber(request.getPhoneNumber().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .dateOfBirth(request.getDateOfBirth())
                .build();

        user = userRepository.save(user);

        // Générer un code de parrainage unique pour chaque nouvel utilisateur
        user.setReferralCode(generateUniqueReferralCode(user));
        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(user)
                .build();
        walletRepository.save(wallet);

        if (user.getRole() == Role.DRIVER) {
            DriverProfile driverProfile = DriverProfile.builder()
                    .user(user)
                    .licenseNumber(generateDriverLicenseNumber(user))
                    .status(DriverStatus.OFFLINE)
                    .verified(false)
                    .build();

            driverProfileRepository.save(driverProfile);
        }

        // Lier le parrainage si un code a été fourni
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            user.setReferredByCode(request.getReferralCode().trim().toUpperCase());
            userRepository.save(user);
            referralService.linkReferral(request.getReferralCode().trim().toUpperCase(), user.getId());
        }

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

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        if (user.getRole() == Role.DRIVER) {
            ensureDriverProfileExists(user);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        SessionToken session = SessionToken.builder()
                .user(user)
                .token(accessToken)
                .deviceInfo(request.getDeviceInfo())
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusSeconds(900))
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

        UUID userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));

        if (user.getRole() == Role.DRIVER) {
            ensureDriverProfileExists(user);
        }

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

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = findUserByIdOrThrow(userId);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = findUserByIdOrThrow(userId);

        validateUniqueEmailAndPhoneForUpdate(
                normalizeNullable(request.getEmail()),
                request.getPhoneNumber(),
                user.getId()
        );

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setDateOfBirth(request.getDateOfBirth());

        user = userRepository.save(user);

        log.info("Profil rider mis à jour pour userId={}", user.getId());
        return mapToResponse(user);
    }

    @Transactional
    public UploadUserProfilePhotoResponse uploadCurrentUserProfilePhoto(UUID userId, MultipartFile file) {
        User user = findUserByIdOrThrow(userId);

        FileStorageService.ImageBlobPayload payload = fileStorageService.extractImageBlobPayload(file);

        user.setProfilePictureData(payload.getData());
        user.setProfilePictureContentType(payload.getContentType());
        user.setProfilePictureFileName(payload.getFileName());
        user.setProfilePictureSize(payload.getSize());

        userRepository.save(user);

        log.info("Photo de profil rider mise à jour pour userId={}, size={} bytes", userId, payload.getSize());

        return UploadUserProfilePhotoResponse.builder()
                .message("Photo de profil mise à jour avec succès")
                .profilePictureUrl(fileStorageService.buildUserProfilePhotoUrl())
                .contentType(payload.getContentType())
                .fileName(payload.getFileName())
                .size(payload.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfilePhotoData getCurrentUserProfilePhoto(UUID userId) {
        User user = findUserByIdOrThrow(userId);

        if (!user.hasProfilePicture()) {
            throw new BusinessException("Aucune photo de profil trouvée pour cet utilisateur");
        }

        return UserProfilePhotoData.builder()
                .data(user.getProfilePictureData())
                .contentType(user.getProfilePictureContentType())
                .fileName(user.getProfilePictureFileName() != null
                        ? user.getProfilePictureFileName()
                        : "profile-photo")
                .size(user.getProfilePictureSize() != null
                        ? user.getProfilePictureSize()
                        : user.getProfilePictureData().length)
                .build();
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
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

    private void ensureDriverProfileExists(User user) {
        driverProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    DriverProfile driverProfile = DriverProfile.builder()
                            .user(user)
                            .licenseNumber(generateDriverLicenseNumber(user))
                            .status(DriverStatus.OFFLINE)
                            .verified(false)
                            .build();

                    DriverProfile saved = driverProfileRepository.save(driverProfile);
                    log.warn("DriverProfile créé automatiquement au login/refresh pour userId={}", user.getId());
                    return saved;
                });
    }

    private String generateUniqueReferralCode(User user) {
        String base = "OVIRO" + user.getFirstName().substring(0, Math.min(2, user.getFirstName().length())).toUpperCase();
        String candidate = base + ThreadLocalRandom.current().nextInt(1000, 9999);
        while (userRepository.findByReferralCode(candidate).isPresent()) {
            candidate = base + ThreadLocalRandom.current().nextInt(1000, 9999);
        }
        return candidate;
    }

    private String generateDriverLicenseNumber(User user) {
        String base = "DRV-" + user.getId().toString().substring(0, 8).toUpperCase();
        String candidate = base;
        int suffix = 1;

        while (driverProfileRepository.findByLicenseNumber(candidate).isPresent()) {
            candidate = base + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private void validateUniqueEmailAndPhoneForCreate(String email, String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé", "PHONE_ALREADY_EXISTS");
        }

        if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
            throw new BusinessException("Cet email est déjà utilisé", "EMAIL_ALREADY_EXISTS");
        }
    }

    private void validateUniqueEmailAndPhoneForUpdate(String email, String phoneNumber, UUID currentUserId) {
        if (userRepository.existsByPhoneNumberAndIdNot(phoneNumber, currentUserId)) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé", "PHONE_ALREADY_EXISTS");
        }

        if (email != null && !email.isBlank() && userRepository.existsByEmailAndIdNot(email, currentUserId)) {
            throw new BusinessException("Cet email est déjà utilisé", "EMAIL_ALREADY_EXISTS");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private User findUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));
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
                .dateOfBirth(user.getDateOfBirth())
                .profilePictureUrl(user.hasProfilePicture() ? fileStorageService.buildUserProfilePhotoUrl() : null)
                .hasProfilePicture(user.hasProfilePicture())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}