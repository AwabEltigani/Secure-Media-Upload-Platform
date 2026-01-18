package com.example.cloud_file_storage.controllers;

import java.util.List;
import java.util.regex.Pattern;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.cloud_file_storage.AwsServices.services.S3Service;
import com.example.cloud_file_storage.dto.request.UploadUrlRequest;
import com.example.cloud_file_storage.dto.response.ApiResponse;
import com.example.cloud_file_storage.dto.response.ImageResponse;
import com.example.cloud_file_storage.dto.response.UploadUrlResponse;
import com.example.cloud_file_storage.enums.ImageStatus;
import com.example.cloud_file_storage.models.Image;
import com.example.cloud_file_storage.models.Users;
import com.example.cloud_file_storage.repos.ImageRepository;
import com.example.cloud_file_storage.services.ImageService;
import com.example.cloud_file_storage.exceptions.ResourceNotFoundException;
import com.example.cloud_file_storage.exceptions.UnauthorizedException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * REST Controller for image management operations.
 * 
 * Handles:
 * - Upload URL generation with presigned S3 URLs
 * - Webhook callbacks from AWS Lambda for scan results
 * - Image listing, retrieval, and deletion
 * 
 * Security:
 * - All user endpoints require JWT authentication
 * - Webhook endpoint requires secret header validation
 * - Users can only access their own images
 * - Rate limiting on webhook to prevent abuse
 * 
 * @author Your Name
 * @version 1.0
 * @since 2026-01-18
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class ImageController {

    private final ImageService imageService;
    private final S3Service s3Service;
    private final ImageRepository imageRepository;

    @Value("${aws.lambda.webhook.secret}")
    private String lambdaSecret;
    
    @Value("${app.upload.max-file-size:104857600}") // 100MB default
    private long maxFileSize;
    
    @Value("${app.upload.max-files-per-user:1000}")
    private int maxFilesPerUser;

    // Rate limiting for webhook endpoint (100 requests per minute)
    private final ConcurrentHashMap<String, Bucket> webhookBuckets = new ConcurrentHashMap<>();
    
    // Valid S3 key pattern (prevent path traversal)
    private static final Pattern VALID_S3_KEY = Pattern.compile("^users/[a-zA-Z0-9@.\\-_]+/[a-zA-Z0-9.\\-_]+$");

    /**
     * Generate presigned S3 URL for file upload.
     * 
     * Creates a database record with SCANNING status and returns a temporary
     * upload URL that expires in 15 minutes.
     * 
     * @param request Upload request containing filename, content type, and size
     * @param user Authenticated user (injected by Spring Security)
     * @return ApiResponse with presigned upload URL and metadata
     * @throws IllegalArgumentException if file size exceeds limit
     * @throws IllegalStateException if user has reached max file limit
     */
    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> getUploadUrl(
            @Valid @RequestBody UploadUrlRequest request,
            @AuthenticationPrincipal Users user) {
        
        log.info("Upload URL request: user={}, file={}, size={}", 
            user.getUsername(), request.filename(), request.fileSize());
        
        // Validate file size
        if (request.fileSize() > maxFileSize) {
            log.warn("File size {} exceeds limit {} for user {}", 
                request.fileSize(), maxFileSize, user.getId());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File size exceeds maximum allowed size of " + 
                    (maxFileSize / 1024 / 1024) + "MB"));
        }
        
        // Check user's total file count
        long userFileCount = imageRepository.countByUserId(user.getId());
        if (userFileCount >= maxFilesPerUser) {
            log.warn("User {} has reached max file limit: {}", user.getId(), maxFilesPerUser);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Maximum file limit reached. Please delete some files."));
        }
        
        // Check if THIS USER already has a file with this name
        if (imageRepository.existsByFilenameAndUserId(request.filename(), user.getId())) {
            log.warn("User {} already has file: {}", user.getId(), request.filename());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("You already have a file with this name"));
        }
        
        try {
            // Generate presigned upload URL
            UploadUrlResponse response = s3Service.generateUploadUrl(
                user.getUsername(),
                request.filename(),
                request.contentType()
            );
            
            // Create image record with SCANNING status
            Image image = Image.builder()
                .s3Key(response.s3Key())
                .contentType(request.contentType())
                .fileSize(request.fileSize())
                .filename(request.filename())
                .status(ImageStatus.SCANNING)
                .userId(user.getId())
                .build();
            
            Image savedImage = imageRepository.save(image);
            
            log.info("Image record created: id={}, s3Key={}, status={}", 
                savedImage.getId(), savedImage.getS3Key(), savedImage.getStatus());
            
            return ResponseEntity.ok(
                ApiResponse.success("Upload URL generated successfully", response)
            );
            
        } catch (Exception e) {
            log.error("Failed to generate upload URL for user {}: {}", 
                user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate upload URL. Please try again."));
        }
    }

    /**
     * INTERNAL - Webhook endpoint for AWS Lambda scan results.
     * 
     * This endpoint is called by Lambda after malware scanning completes.
     * Updates the image status in the database based on scan results.
     * 
     * Security:
     * - Requires X-Internal-Secret header matching configured secret
     * - Rate limited to 100 requests per minute
     * - Validates S3 key format to prevent injection attacks
     * 
     * @param secret Webhook secret from Lambda (must match configured secret)
     * @param s3Key S3 object key of the scanned file
     * @param status Scan result status (CLEAN or THREAT_DETECTED)
     * @return ApiResponse indicating success or failure
     */
    @PostMapping("/internal/scan-result")
    public ResponseEntity<ApiResponse<Void>> updateScanResult(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestParam @NotBlank String s3Key,
            @RequestParam @NotBlank String status) {
        
        log.info("Webhook received: s3Key={}, status={}, hasSecret={}", 
            s3Key, status, secret != null);
        
        // Rate limiting check
        Bucket bucket = webhookBuckets.computeIfAbsent("webhook", k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build()
        );
        
        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for webhook endpoint");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Rate limit exceeded"));
        }
        
        // Validate webhook secret
        if (secret == null || secret.isBlank()) {
            log.warn("Webhook attempt with missing secret for s3Key: {}", s3Key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Missing authentication secret"));
        }
        
        if (!lambdaSecret.equals(secret)) {
            log.warn("Webhook attempt with invalid secret for s3Key: {}", s3Key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid authentication secret"));
        }
        
        // Validate S3 key format (prevent injection)
        if (!VALID_S3_KEY.matcher(s3Key).matches()) {
            log.error("Invalid S3 key format: {}", s3Key);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid S3 key format"));
        }
        
        // Validate status value
        if (!status.equalsIgnoreCase("CLEAN") && !status.equalsIgnoreCase("THREAT_DETECTED")) {
            log.error("Invalid status value: {}", status);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid status value. Must be CLEAN or THREAT_DETECTED"));
        }

        try {
            // Update image status based on scan result
            imageService.handleScanResult(s3Key, status.toUpperCase());
            
            log.info("Successfully processed scan result for s3Key: {} -> {}", s3Key, status);
            return ResponseEntity.ok(
                ApiResponse.success("Scan result processed successfully", null)
            );
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value during processing: {}", status, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid status value: " + e.getMessage()));
                
        } catch (ResourceNotFoundException e) {
            log.error("Image not found for s3Key: {}", s3Key, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Image record not found for key: " + s3Key));
                
        } catch (Exception e) {
            log.error("Unexpected error processing scan result for s3Key: {}", s3Key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to process scan result"));
        }
    }

    /**
     * Get all images for the authenticated user.
     * 
     * Returns list of images with metadata including filename, upload date,
     * size, and current status (SCANNING, CLEAN, THREAT_DETECTED).
     * 
     * @param user Authenticated user
     * @return ApiResponse with list of user's images
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getUserImages(
            @AuthenticationPrincipal Users user) {
        
        log.debug("Fetching images for user: {}", user.getId());
        
        try {
            List<ImageResponse> images = imageService.getUserImages(user.getId());
            
            log.info("Retrieved {} images for user {}", images.size(), user.getId());
            return ResponseEntity.ok(
                ApiResponse.success("Images retrieved successfully", images)
            );
            
        } catch (Exception e) {
            log.error("Failed to retrieve images for user {}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve images"));
        }
    }

    /**
     * Get image status and download URL.
     * 
     * Returns image metadata including current scan status. If the image is CLEAN,
     * includes a presigned download URL valid for 15 minutes.
     * 
     * @param id Image ID
     * @param user Authenticated user
     * @return ApiResponse with image details and download URL (if clean)
     * @throws UnauthorizedException if user doesn't own the image
     * @throws ResourceNotFoundException if image doesn't exist
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ImageResponse>> getImageStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal Users user) {
        
        log.debug("Fetching status for image {} by user {}", id, user.getId());
        
        // Verify ownership
        if (!imageRepository.existsByIdAndUserId(id, user.getId())) {
            log.warn("User {} attempted to access image {} without permission", user.getId(), id);
            throw new UnauthorizedException("You don't have permission to access this image");
        }
        
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + id));
        
        ImageResponse response = ImageResponse.from(image);
        
        // Add download URL if file is clean and available
        if (image.getStatus() == ImageStatus.CLEAN) {
            try {
                String downloadUrl = s3Service.generateDownloadUrl(
                    image.getS3Key(), 
                    user.getUsername(), 
                    15
                );
                response = response.withDownloadUrl(downloadUrl);
                log.debug("Generated download URL for image {}", id);
                
            } catch (Exception e) {
                log.error("Failed to generate download URL for image {}: {}", id, e.getMessage());
                // Continue without download URL rather than failing the entire request
            }
        }
        
        return ResponseEntity.ok(
            ApiResponse.success("Image status retrieved successfully", response)
        );
    }

    /**
     * Delete an image.
     * 
     * Removes the image from both database and S3 storage.
     * Only the owner can delete their images.
     * 
     * @param id Image ID to delete
     * @param user Authenticated user
     * @return ApiResponse confirming deletion
     * @throws UnauthorizedException if user doesn't own the image
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @AuthenticationPrincipal Users user) {
        
        log.info("Delete request for image {} by user {}", id, user.getId());
        
        try {
            imageService.deleteImage(id, user.getId());
            
            log.info("Successfully deleted image {} for user {}", id, user.getId());
            return ResponseEntity.ok(
                ApiResponse.success("Image deleted successfully", null)
            );
            
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized delete attempt: user {} tried to delete image {}", 
                user.getId(), id);
            throw e;
            
        } catch (ResourceNotFoundException e) {
            log.warn("Delete failed: image {} not found", id);
            throw e;
            
        } catch (Exception e) {
            log.error("Failed to delete image {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete image"));
        }
    }
    
    /**
     * Get image count for the authenticated user.
     * 
     * Useful for displaying quota information in the UI.
     * 
     * @param user Authenticated user
     * @return ApiResponse with user's total image count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserImageCount(
            @AuthenticationPrincipal Users user) {
        
        long count = imageRepository.countByUserId(user.getId());
        
        log.debug("User {} has {} images", user.getId(), count);
        return ResponseEntity.ok(
            ApiResponse.success("Image count retrieved", count)
        );
    }
}