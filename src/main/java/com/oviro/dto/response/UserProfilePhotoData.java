package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfilePhotoData {
    private byte[] data;
    private String contentType;
    private String fileName;
    private long size;
}