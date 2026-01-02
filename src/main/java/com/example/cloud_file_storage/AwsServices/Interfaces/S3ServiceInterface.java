package com.example.cloud_file_storage.AwsServices.Interfaces;

import java.util.Map;
import com.example.cloud_file_storage.dto.response.UploadUrlResponse;

public interface S3ServiceInterface {

    // Version for Controller (returns DTO with Key)
    UploadUrlResponse generateUploadUrl(String username, String originalFileName, String contentType);

    // Version with custom expiration (returns DTO)
    UploadUrlResponse generateUploadUrl(String username, String fileName, String contentType, int expirationMinutes);

    // Version returning a simple String (matching your line 248)
    String generateUploadUrl(String fileName, String contentType, int durationAvaliableInMinutes);

    // Download logic
    String generateDownloadUrl(String s3Key, String username, int expirationMinutes);

    // Bucket Management
    void moveFileToPermanentBucket(String s3Key);

    void deleteFile(String bucketName, String s3Key);

    void deleteFromQuarantine(String s3Key);

    void deleteFromPermanent(String s3Key);

    // Metadata & Checks
    boolean fileExists(String bucketName, String s3Key);

    Map<String, String> getFileMetadata(String bucketName, String s3Key);
}