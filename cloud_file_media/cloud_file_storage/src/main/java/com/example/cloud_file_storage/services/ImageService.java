package com.example.cloud_file_storage.services;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

// AWS SDK v2 Imports
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

// Spring & Utilities
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

// Project Specific
import com.example.cloud_file_storage.AwsServices.Interfaces.S3ServiceInterface;
import com.example.cloud_file_storage.dto.request.SaveImageRequest;
import com.example.cloud_file_storage.dto.response.ImageResponse;
import com.example.cloud_file_storage.enums.ImageStatus;
import com.example.cloud_file_storage.exceptions.ResourceNotFoundException;
import com.example.cloud_file_storage.exceptions.UnauthorizedException;
import com.example.cloud_file_storage.models.Image;
import com.example.cloud_file_storage.repos.ImageRepository;

@Service
@Slf4j
@Transactional
public class ImageService {

    private final ImageRepository imageRepository;
    private final S3ServiceInterface s3Service;

    public ImageService(ImageRepository imageRepository, S3ServiceInterface s3Service) {
        this.imageRepository = imageRepository;
        this.s3Service = s3Service;
    }

    /** Save image metadata after S3 upload (not the file) */
    public ImageResponse saveImage(SaveImageRequest request, Long userId) {
        log.info("Saving image metadata for user: {}, file: {}", userId, request.fileName());

        Image image = Image.builder()
                .filename(request.fileName())
                .fileSize(request.fileSize())
                .contentType(request.contentType())
                .s3Key(request.s3Key())
                .userId(userId)
                .status(ImageStatus.SCANNING)
                .build();

        Image savedImage = imageRepository.save(image);
        log.info("Saved image metadata with ID: {}", savedImage.getId());

        return ImageResponse.from(savedImage);
    }

    /**
     * Handles scan result from Lambda webhook
     * Updates database status only - Lambda already moved/deleted files
     */
    public void handleScanResult(String s3Key, String statusString) {
        log.info("Processing scan result for S3 key: {}, status: {}", s3Key, statusString);

        // Convert string to enum
        ImageStatus status;
        try {
            status = ImageStatus.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", statusString);
            throw new IllegalArgumentException("Invalid status: " + statusString);
        }

        Image image = imageRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new ResourceNotFoundException("Image with S3 key not found: " + s3Key));

        image.setStatus(status);
        imageRepository.save(image);
        
        log.info("Successfully updated image status to {} for s3Key: {}", status, s3Key);
        // Note: Lambda already handled file move/delete operations
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getUserImages(Long userId) {
        log.debug("Fetching images for user: {}", userId);
        return imageRepository.findByUserId(userId).stream()
                .map(ImageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImageResponse getImageWithDownloadUrl(Long imageId, Long userId, String username) {
        log.debug("Fetching image: {} for user: {}", imageId, userId);
        Image image = getImageAndVerifyOwnership(imageId, userId);
        String downloadUrl = "";
        
        if (image.getStatus() == ImageStatus.CLEAN) {
            downloadUrl = s3Service.generateDownloadUrl(image.getS3Key(), username, 15);
        }
        return ImageResponse.from(image).withDownloadUrl(downloadUrl);
    }

    @Transactional(readOnly = true)
    public Image getImage(Long imageId, Long userId) {
        return getImageAndVerifyOwnership(imageId, userId);
    }

    public void deleteImage(Long imageId, Long userId) {
        log.info("Deleting image: {} for user: {}", imageId, userId);
        Image image = getImageAndVerifyOwnership(imageId, userId);

        try {
            if (image.getStatus() == ImageStatus.CLEAN) {
                s3Service.deleteFromPermanent(image.getS3Key());
            } else {
                s3Service.deleteFromQuarantine(image.getS3Key());
            }
        } catch (Exception e) {
            log.warn("Failed to delete from S3, continuing with DB deletion", e);
        }

        imageRepository.delete(image);
        log.info("Successfully deleted image: {}", imageId);
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getAllImages() {
        log.debug("Fetching all images (admin)");
        return imageRepository.findAll().stream()
                .map(ImageResponse::from)
                .collect(Collectors.toList());
    }

    private Image getImageAndVerifyOwnership(Long imageId, Long userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image with id: " + imageId + " was not found"));

        if (!image.getUserId().equals(userId)) {
            log.warn("Unauthorized access attempt: user {} tried to access image {}", userId, imageId);
            throw new UnauthorizedException("You don't have permission to access this image");
        }
        return image;
    }
}