package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadDriverProfilePhotoResponse {
    private String fileName;
    private String contentType;
    private long size;
    private String viewUrl;
}