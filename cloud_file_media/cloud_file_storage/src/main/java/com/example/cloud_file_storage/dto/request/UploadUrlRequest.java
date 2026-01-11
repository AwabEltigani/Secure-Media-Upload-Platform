package com.example.cloud_file_storage.dto.request;



public record UploadUrlRequest(
    String filename,
    String contentType,
    Long fileSize
) {}