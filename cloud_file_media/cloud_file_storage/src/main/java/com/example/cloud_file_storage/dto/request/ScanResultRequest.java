package com.example.cloud_file_storage.dto.request;

public record ScanResultRequest(
    String s3Key,
    String status  
) {}