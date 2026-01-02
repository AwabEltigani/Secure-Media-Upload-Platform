package com.example.cloud_file_storage.exceptions;

import com.example.cloud_file_storage.enums.ImageStatus;

public class ResourceNotFoundException extends RuntimeException{
    
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }

    public ResourceNotFoundException(String s3Key, ImageStatus status) {
        super(String.format("Resource not found with S3 key: %s and status: %s", s3Key, status));
    }


    
}
