package com.oviro.dto.response;

import com.oviro.enums.Role;
import com.oviro.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private LocalDate dateOfBirth;
    private String profilePictureUrl;
    private boolean hasProfilePicture;
    private boolean emailVerified;
    private boolean phoneVerified;
    private LocalDateTime createdAt;
}