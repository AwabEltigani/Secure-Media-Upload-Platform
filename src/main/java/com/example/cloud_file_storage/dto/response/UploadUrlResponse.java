package com.example.cloud_file_storage.dto.response;

public record UploadUrlResponse(
    String uploadUrl,
    String s3Key,
    int expiresInMinutes
) {
    public UploadUrlResponse(String uploadUrl, String s3Key){
        this(uploadUrl, s3Key, 15);
    }
}
