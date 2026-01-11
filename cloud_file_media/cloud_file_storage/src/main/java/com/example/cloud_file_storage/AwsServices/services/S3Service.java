package com.example.cloud_file_storage.AwsServices.services;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.io.SdkDigestInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import com.example.cloud_file_storage.AwsServices.Interfaces.S3ServiceInterface;
import com.example.cloud_file_storage.dto.response.UploadUrlResponse;
import com.example.cloud_file_storage.exceptions.S3OperationException;
import com.example.cloud_file_storage.exceptions.InvalidFileException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service implements S3ServiceInterface{

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.quarantine-bucket}")
    private String quarantineBucket;

    @Value("${aws.s3.permanent-bucket}")
    private String permanentBucket;

    @Value("${aws.s3.presigned-url-expiration:15}")
    private int defaultExpirationMinutes;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /**
     * Generate a pre-signed upload URL for the quarantine bucket
     */
    public UploadUrlResponse generateUploadUrl(String username, String originalFileName, String contentType) {
        log.info("Generating upload URL for user: {}, file: {}", username, originalFileName);

        validateFileType(originalFileName, contentType);
        String fileExtension = extractFileExtension(originalFileName);
        String s3Key = buildS3Key(username, fileExtension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(quarantineBucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(defaultExpirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                    .url()
                    .toString();
            log.info("Generated upload URL for S3 key: {}", s3Key);
            return new UploadUrlResponse(uploadUrl, s3Key, defaultExpirationMinutes);
        } catch (Exception e) {
            log.error("Failed to generate upload URL for user: {}", username, e);
            throw new S3OperationException("Failed to generate upload URL", e);
        }
    }

    /**
     * Generate a pre-signed download URL
     */
    public String generateDownloadUrl(String s3Key, String username, int expirationMinutes) {
        log.info("Generating download URL for key: {}, user: {}, expiration: {} minutes", 
                s3Key, username, expirationMinutes);

        if (!s3Key.startsWith("users/" + username + "/")) {
            log.warn("Unauthorized download attempt: user {} tried to access {}", username, s3Key);
            throw new SecurityException("Unauthorized access to file");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(permanentBucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            String downloadUrl = s3Presigner.presignGetObject(presignRequest)
                    .url()
                    .toString();
            log.info("Generated download URL for key: {}", s3Key);
            return downloadUrl;
        } catch (Exception e) {
            log.error("Failed to generate download URL for key: {}", s3Key, e);
            throw new S3OperationException("Failed to generate download URL", e);
        }
    }

    /**
     * Move file from quarantine to permanent bucket
     */
    public void moveFileToPermanentBucket(String s3Key) {
        log.info("Moving file to permanent bucket: {}", s3Key);
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(quarantineBucket)
                    .sourceKey(s3Key)
                    .destinationBucket(permanentBucket)
                    .destinationKey(s3Key)
                    .build();

            s3Client.copyObject(copyRequest);
            log.info("Copied file to permanent bucket: {}", s3Key);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(quarantineBucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted file from quarantine bucket: {}", s3Key);
        } catch (S3Exception e) {
            log.error("Failed to move file: {}", s3Key, e);
            throw new S3OperationException("Failed to move file to permanent storage", e);
        }
    }

    /**
     * Delete file from specified bucket
     */
    public void deleteFile(String bucketName, String s3Key) {
        log.info("Deleting file: {} from bucket: {}", s3Key, bucketName);
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted file: {}", s3Key);
        } catch (S3Exception e) {
            log.error("Failed to delete file: {}", s3Key, e);
            throw new S3OperationException("Failed to delete file", e);
        }
    }

    public void deleteFromQuarantine(String s3Key) {
        deleteFile(quarantineBucket, s3Key);
    }

    public void deleteFromPermanent(String s3Key) {
        deleteFile(permanentBucket, s3Key);
    }

    public boolean fileExists(String bucketName, String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("Error checking file existence: {}", s3Key, e);
            throw new S3OperationException("Error checking file existence", e);
        }
    }

    public Map<String, String> getFileMetadata(String bucketName, String s3Key) {
        log.debug("Getting metadata for file: {}", s3Key);
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build()
            );

            Map<String, String> metadata = new HashMap<>();
            metadata.putAll(response.metadata());
            metadata.put("contentType", response.contentType());
            metadata.put("contentLength", String.valueOf(response.contentLength()));
            metadata.put("lastModified", response.lastModified().toString());
            metadata.put("eTag", response.eTag());

            return metadata;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.error("File not found: {}", s3Key);
                throw new S3OperationException("File does not exist: " + s3Key);
            }
            log.error("Failed to get metadata for file: {}", s3Key, e);
            throw new S3OperationException("Failed to get file metadata", e);
        }
    }

    private String buildS3Key(String username, String fileExtension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("users/%s/%s_%s%s", username, timestamp, uniqueId, fileExtension);
    }

    private String extractFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("Invalid filename: no extension found");
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private void validateFileType(String filename, String contentType) {
        String extension = extractFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("Invalid file extension: " + extension + ". Allowed: " + ALLOWED_EXTENSIONS);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("Invalid content type: " + contentType + ". Allowed: " + ALLOWED_CONTENT_TYPES);
        }
    }

    public String getQuarantineBucket() {
        return quarantineBucket;
    }

    public String getPermanentBucket() {
        return permanentBucket;
    }

    @Override
    public UploadUrlResponse generateUploadUrl(String username, String fileName, String contentType, int expirationMinutes) {
    log.info("Generating upload URL for user: {} with custom expiration: {}", username, expirationMinutes);
    
    // Logic to use custom expiration
    String fileExtension = extractFileExtension(fileName);
    String s3Key = buildS3Key(username, fileExtension);

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(quarantineBucket)
            .key(s3Key)
            .contentType(contentType)
            .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .putObjectRequest(putObjectRequest)
            .build();

    String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
    return new UploadUrlResponse(uploadUrl, s3Key, expirationMinutes);
}

    @Override
public String generateUploadUrl(String fileName, String contentType, int durationAvaliableInMinutes) {
    log.info("Generating simple upload URL for file: {} (Duration: {} min)", fileName, durationAvaliableInMinutes);

    // 1. Build the S3 Key (You can customize the 'username' part or use 'default')
    String fileExtension = extractFileExtension(fileName);
    String s3Key = buildS3Key("uploads", fileExtension); 

    // 2. Configure the Put Request
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(quarantineBucket)
            .key(s3Key)
            .contentType(contentType)
            .build();

    // 3. Create the Presign Request
    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(durationAvaliableInMinutes))
            .putObjectRequest(putObjectRequest)
            .build();

    try {
        // 4. Return just the URL String to match the interface signature
        return s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    } catch (Exception e) {
        log.error("Failed to generate simple upload URL", e);
        throw new S3OperationException("Failed to generate upload URL", e);
    }
}

}
