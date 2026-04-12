package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadUserProfilePhotoResponse {
    private String message;
    private String profilePictureUrl;
    private String contentType;
    private String fileName;
    private long size;
}