package com.oviro.service;

import com.oviro.exception.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadRoot);
            log.info("Upload root initialized at {}", this.uploadRoot);
        } catch (IOException e) {
            throw new BusinessException("Impossible d'initialiser le dossier de stockage");
        }
    }

    public String storeDriverProfilePhoto(MultipartFile file, UUID userId) {
        validateImage(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "photo.jpg" : file.getOriginalFilename()
        );

        String extension = getExtension(originalName);
        String generatedName = "driver_" + userId + "_" + UUID.randomUUID() + extension;

        Path targetDir = getDriverProfileDirectory();
        Path targetFile = targetDir.resolve(generatedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Driver profile photo stored at {}", targetFile);
        } catch (IOException e) {
            log.error("Failed to store driver profile photo", e);
            throw new BusinessException("Impossible de sauvegarder la photo de profil");
        }

        return generatedName;
    }

    public Resource loadDriverProfilePhotoAsResource(String fileName) {
        try {
            Path filePath = getDriverProfileDirectory().resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Photo de profil introuvable");
            }

            return resource;
        } catch (Exception e) {
            log.error("Unable to load profile photo {}", fileName, e);
            throw new BusinessException("Impossible de lire la photo de profil");
        }
    }

    public String buildDriverProfilePhotoPublicUrl(String fileName) {
        return "/public/files/profile-photo/" + fileName;
    }

    public ImageBlobPayload extractImageBlobPayload(MultipartFile file) {
        validateImage(file);

        try {
            return ImageBlobPayload.builder()
                    .data(file.getBytes())
                    .contentType(normalizeContentType(file.getContentType()))
                    .fileName(cleanOriginalFileName(file.getOriginalFilename()))
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("Failed to read uploaded image into memory", e);
            throw new BusinessException("Impossible de lire le fichier image");
        }
    }

    public String buildUserProfilePhotoUrl() {
        return "/auth/me/profile-photo";
    }

    private Path getDriverProfileDirectory() {
        return uploadRoot.resolve("drivers").resolve("profiles");
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier image est obligatoire");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("La photo de profil ne doit pas dépasser 5 MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("Format image non supporté. Utilise JPG, PNG ou WEBP");
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.trim().toLowerCase();
    }

    private String cleanOriginalFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName == null ? "profile-photo.jpg" : originalFileName);
        if (cleaned.isBlank()) {
            return "profile-photo.jpg";
        }
        return cleaned;
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return ".jpg";
        }
        return fileName.substring(idx).toLowerCase();
    }

    @Data
    @Builder
    public static class ImageBlobPayload {
        private byte[] data;
        private String contentType;
        private String fileName;
        private long size;
    }
}