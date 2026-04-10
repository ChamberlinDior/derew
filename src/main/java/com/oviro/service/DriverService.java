package com.oviro.service;

import com.oviro.dto.request.DriverLocationRequest;
import com.oviro.dto.request.SosAlertRequest;
import com.oviro.dto.request.UpdateDriverProfileRequest;
import com.oviro.dto.response.DriverProfileResponse;
import com.oviro.dto.response.UploadDriverProfilePhotoResponse;
import com.oviro.enums.DriverStatus;
import com.oviro.enums.Role;
import com.oviro.enums.RideStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.DriverProfile;
import com.oviro.model.Ride;
import com.oviro.model.SosAlert;
import com.oviro.model.User;
import com.oviro.model.Vehicle;
import com.oviro.repository.DriverProfileRepository;
import com.oviro.repository.RideRepository;
import com.oviro.repository.SosAlertRepository;
import com.oviro.repository.UserRepository;
import com.oviro.repository.VehicleRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final DriverProfileRepository driverProfileRepository;
    private final RideRepository rideRepository;
    private final SosAlertRepository sosAlertRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final SecurityContextHelper securityHelper;

    @Transactional
    public void setOnlineStatus(boolean online) {
        DriverProfile driver = getOrCreateMyProfileEntity();

        if (!online && driver.getStatus() == DriverStatus.ON_RIDE) {
            throw new BusinessException("Impossible de passer hors-ligne pendant une course active");
        }

        driver.setStatus(online ? DriverStatus.ONLINE : DriverStatus.OFFLINE);
        driverProfileRepository.save(driver);
        log.info("Chauffeur {} -> {}", driver.getUser().getId(), driver.getStatus());
    }

    @Transactional
    public void updateLocation(DriverLocationRequest request) {
        DriverProfile driver = getOrCreateMyProfileEntity();
        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driverProfileRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public Page<Ride> getAvailableRides(BigDecimal lat, BigDecimal lng, Pageable pageable) {
        return rideRepository.findByStatus(RideStatus.REQUESTED, pageable);
    }

    @Transactional
    public SosAlert triggerSos(SosAlertRequest request) {
        DriverProfile driver = getOrCreateMyProfileEntity();

        driver.setStatus(DriverStatus.SOS);
        driverProfileRepository.save(driver);

        Ride ride = null;
        if (request.getRideId() != null) {
            ride = rideRepository.findById(request.getRideId()).orElse(null);
        }

        SosAlert alert = SosAlert.builder()
                .driver(driver)
                .ride(ride)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .build();

        return sosAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getNearbyDrivers(BigDecimal lat, BigDecimal lng, double radiusKm) {
        return driverProfileRepository.findNearbyOnlineDrivers(lat, lng, radiusKm);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getMyProfile() {
        DriverProfile profile = getOrCreateMyProfileEntity();
        return mapToResponse(profile);
    }

    @Transactional
    public DriverProfileResponse updateMyProfile(UpdateDriverProfileRequest request) {
        DriverProfile profile = getOrCreateMyProfileEntity();
        User user = profile.getUser();

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().isBlank() ? null : request.getEmail().trim().toLowerCase());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth().atStartOfDay());
        }

        if (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) {
            profile.setLicenseNumber(request.getLicenseNumber().trim());
        }

        if (request.getLicenseExpiryDate() != null) {
            profile.setLicenseExpiryDate(request.getLicenseExpiryDate());
        }

        if (request.getNationalId() != null) {
            profile.setNationalId(request.getNationalId().isBlank() ? null : request.getNationalId().trim());
        }

        boolean hasVehicleData =
                hasText(request.getVehicleMake()) ||
                        hasText(request.getVehicleModel()) ||
                        hasText(request.getPlateNumber()) ||
                        hasText(request.getVehicleColor()) ||
                        hasText(request.getVehicleType()) ||
                        request.getVehicleYear() != null ||
                        request.getVehicleSeats() != null;

        if (hasVehicleData) {
            Vehicle vehicle = profile.getCurrentVehicle();

            if (vehicle == null) {
                vehicle = Vehicle.builder().build();
            }

            if (hasText(request.getVehicleMake())) {
                vehicle.setMake(request.getVehicleMake().trim());
            }
            if (hasText(request.getVehicleModel())) {
                vehicle.setModel(request.getVehicleModel().trim());
            }
            if (hasText(request.getPlateNumber())) {
                vehicle.setPlateNumber(request.getPlateNumber().trim().toUpperCase());
            }
            if (hasText(request.getVehicleColor())) {
                vehicle.setColor(request.getVehicleColor().trim());
            }
            if (hasText(request.getVehicleType())) {
                vehicle.setType(request.getVehicleType().trim());
            }
            if (request.getVehicleYear() != null) {
                vehicle.setYear(request.getVehicleYear());
            }
            if (request.getVehicleSeats() != null) {
                vehicle.setSeats(request.getVehicleSeats());
            }

            vehicle = vehicleRepository.save(vehicle);
            profile.setCurrentVehicle(vehicle);
        }

        userRepository.save(user);
        DriverProfile saved = driverProfileRepository.save(profile);

        return mapToResponse(saved);
    }

    @Transactional
    public UploadDriverProfilePhotoResponse uploadProfilePhoto(MultipartFile file) {
        validateImage(file);

        DriverProfile profile = getOrCreateMyProfileEntity();

        try {
            profile.setProfilePhotoData(file.getBytes());
            profile.setProfilePhotoContentType(file.getContentType());
            profile.setProfilePhotoFileName(file.getOriginalFilename());
            profile.setProfilePhotoSize(file.getSize());

            driverProfileRepository.save(profile);

            return UploadDriverProfilePhotoResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .viewUrl("/driver/profile/photo")
                    .build();

        } catch (IOException e) {
            log.error("Impossible de lire le fichier image pour la sauvegarde BLOB", e);
            throw new BusinessException("Impossible de sauvegarder la photo de profil en base");
        }
    }

    @Transactional(readOnly = true)
    public DriverProfile getMyProfileEntity() {
        return getOrCreateMyProfileEntity();
    }

    @Transactional
    public DriverProfile getOrCreateMyProfileEntity() {
        UUID userId = securityHelper.getCurrentUserId();

        return driverProfileRepository.findByUserId(userId)
                .orElseGet(() -> createMissingDriverProfile(userId));
    }

    private DriverProfile createMissingDriverProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        if (user.getRole() != Role.DRIVER) {
            throw new BusinessException("L'utilisateur connecté n'a pas le rôle DRIVER");
        }

        DriverProfile profile = DriverProfile.builder()
                .user(user)
                .licenseNumber(generateDriverLicenseNumber(user))
                .status(DriverStatus.OFFLINE)
                .verified(false)
                .build();

        return driverProfileRepository.save(profile);
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

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier image est obligatoire");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("La photo de profil ne doit pas dépasser 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Format image non supporté. Utilise JPG, PNG ou WEBP");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private DriverProfileResponse mapToResponse(DriverProfile profile) {
        return DriverProfileResponse.builder()
                .id(profile.getId())
                .user(mapUser(profile))
                .licenseNumber(profile.getLicenseNumber())
                .licenseExpiryDate(profile.getLicenseExpiryDate())
                .nationalId(profile.getNationalId())
                .status(profile.getStatus())
                .currentLatitude(profile.getCurrentLatitude())
                .currentLongitude(profile.getCurrentLongitude())
                .rating(profile.getRating())
                .totalRides(profile.getTotalRides())
                .totalEarnings(profile.getTotalEarnings())
                .verified(profile.isVerified())
                .currentVehicle(mapVehicle(profile.getCurrentVehicle()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private DriverProfileResponse.UserSummary mapUser(DriverProfile profile) {
        User user = profile.getUser();
        if (user == null) {
            return null;
        }

        return DriverProfileResponse.UserSummary.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(profile.getProfilePhotoData() != null ? "/driver/profile/photo" : null)
                .dateOfBirth(user.getDateOfBirth())
                .build();
    }

    private DriverProfileResponse.VehicleSummary mapVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return DriverProfileResponse.VehicleSummary.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .type(vehicle.getType())
                .seats(vehicle.getSeats())
                .status(vehicle.getStatus() != null ? vehicle.getStatus().name() : null)
                .build();
    }
}