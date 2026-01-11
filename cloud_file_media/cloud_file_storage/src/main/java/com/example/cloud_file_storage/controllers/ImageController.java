package com.example.cloud_file_storage.controllers;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import com.example.cloud_file_storage.AwsServices.services.S3Service;
import com.example.cloud_file_storage.dto.request.SaveImageRequest;
import com.example.cloud_file_storage.dto.request.UploadUrlRequest;
import com.example.cloud_file_storage.dto.response.ApiResponse;
import com.example.cloud_file_storage.dto.response.ImageResponse;
import com.example.cloud_file_storage.dto.response.UploadUrlResponse;
import com.example.cloud_file_storage.enums.ImageStatus;
import com.example.cloud_file_storage.models.Image;
import com.example.cloud_file_storage.models.Users;
import com.example.cloud_file_storage.repos.ImageRepository;
import com.example.cloud_file_storage.repos.UsersRepository;
import com.example.cloud_file_storage.services.ImageService;
import com.example.cloud_file_storage.exceptions.ResourceNotFoundException;
import com.example.cloud_file_storage.exceptions.UnauthorizedException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class ImageController {

    private final ImageService imageService;
    private final S3Service s3Service;
    private final ImageRepository imageRepository;
    private final UsersRepository usersRepository;

    @Value("${aws.lambda.webhook.secret}")
    private String lambdaSecret;

    /**
     * Request S3 Presigned URL
     */
    @PostMapping("/upload-url")
public ResponseEntity<?> getUploadUrl(  // Changed to <?> to allow error responses
        @Valid @RequestBody UploadUrlRequest request,
        @AuthenticationPrincipal Users user) {
    
    log.info("Upload URL request: user={}, file={}", user.getUsername(), request.filename());
    
    // Check for duplicate filename FIRST (before generating URL)
    if (imageRepository.existsByFilename(request.filename())) {
        log.warn("Duplicate filename detected: {}", request.filename());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "File already exists",
                "filename", request.filename()
            ));
    }
    
    // Generate presigned upload URL
    UploadUrlResponse response = s3Service.generateUploadUrl(
        user.getUsername(),
        request.filename(),
        request.contentType()
    );
    
    // Create image record with SCANNING status
    Image image = Image.builder()  // Using builder is cleaner
        .s3Key(response.s3Key())
        .contentType(request.contentType())
        .fileSize(request.fileSize())  // Use actual file size from request
        .filename(request.filename())
        .status(ImageStatus.SCANNING)
        .userId(user.getId())
        .build();
    
    imageRepository.save(image);
    
    log.info("Image record created: id={}, s3Key={}, status={}", 
        image.getId(), image.getS3Key(), image.getStatus());
    
    return ResponseEntity.ok(response);
}

    /**
     * Save initial metadata (Status: SCANNING)
     */
    @PostMapping
    public ResponseEntity<ImageResponse> saveImage(
            @Valid @RequestBody SaveImageRequest request,
            @AuthenticationPrincipal Users user) {
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageService.saveImage(request, user.getId()));
    }

    /**
     *  INTERNAL Webhook called by AWS Lambda
     * EXISTING record and moves file in S3
     */
    @PostMapping("/internal/scan-result")
    public ResponseEntity<ApiResponse<Void>> updateScanResult(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestParam String s3Key,
            @RequestParam String status) {
        
        //Security Check: Validate Secret from Env Variables
        if (!lambdaSecret.equals(secret)) {
            log.warn("Unauthorized webhook attempt for s3Key: {}", s3Key);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            //Logic Check: Convert string to Enum and update existing record
            imageService.handleScanResult(s3Key, status.toUpperCase());
            
            return ResponseEntity.ok(ApiResponse.success("Scan processed successfully", null));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status value"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Image record not found"));
        }
    }

    @GetMapping
    public ResponseEntity<List<ImageResponse>> getUserImages(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(imageService.getUserImages(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @AuthenticationPrincipal Users user) {
        imageService.deleteImage(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Image deleted", null));
    }

@GetMapping("/api/images/{id}/status")
public ResponseEntity<ImageResponse> getScanStatus(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    // Get current user
    Users currentUser = usersRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Check ownership first (more efficient)
    if (!imageRepository.existsByIdAndUserId(id, currentUser.getId())) {
        throw new UnauthorizedException("You don't have permission to view this image");
    }
    
    Image image = imageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
    
    ImageResponse response = ImageResponse.from(image);
    
    // Only include download URL if file is clean and available
    if (image.getStatus() == ImageStatus.CLEAN) {
        // Use generateDownloadUrl with username validation (15 min expiration)
        String downloadUrl = s3Service.generateDownloadUrl(
            image.getS3Key(), 
            currentUser.getUsername(), 
            15
        );
        response = response.withDownloadUrl(downloadUrl);
        
        // Optional: Add thumbnail URL if you have thumbnails
        // if (image.getThumbnailS3Key() != null) {. fix this later
        //     String thumbnailUrl = s3Service.generateDownloadUrl(
        //         String.valueOf(image.getThumbnailS3Key()),
        //         currentUser.getUsername(), 
        //         15
        //     );
            // response = response.withThumbnailUrl(thumbnailUrl);
        // }
    }
    
    return ResponseEntity.ok(response);
}

}

