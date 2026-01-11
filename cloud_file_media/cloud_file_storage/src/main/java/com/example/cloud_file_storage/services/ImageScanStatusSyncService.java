package com.example.cloud_file_storage.services;

import com.example.cloud_file_storage.AwsServices.Interfaces.S3ServiceInterface;
import com.example.cloud_file_storage.enums.ImageStatus;
import com.example.cloud_file_storage.models.Image;
import com.example.cloud_file_storage.repos.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageScanStatusSyncService {
    
    private final ImageRepository imageRepository;
    private final S3ServiceInterface s3Service;
    
    @Value("${aws.s3.permanent-bucket}")
    private String permanentBucket;
    
    @Value("${aws.s3.quarantine-bucket}")
    private String quarantineBucket;
    
    // How long to wait before checking (give user time to upload)
    private static final int UPLOAD_GRACE_PERIOD_MINUTES = 5;
    
    // How long before considering a scan stuck/failed
    private static final int SCAN_TIMEOUT_MINUTES = 15;
    
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void syncImageStatus() {
        log.info("Starting image scan status sync");
        
        // Only check images that have been in SCANNING status for at least 5 minutes
        LocalDateTime gracePeriodCutoff = LocalDateTime.now()
            .minusMinutes(UPLOAD_GRACE_PERIOD_MINUTES);
        
        List<Image> scanningImages = imageRepository
            .findByStatusAndUploadDateBefore(ImageStatus.SCANNING, gracePeriodCutoff);
        
        if (scanningImages.isEmpty()) {
            log.debug("No images in SCANNING status older than {} minutes", UPLOAD_GRACE_PERIOD_MINUTES);
            return;
        }
        
        log.info("Found {} images in SCANNING status (older than {} min)", 
            scanningImages.size(), UPLOAD_GRACE_PERIOD_MINUTES);
        
        for (Image image : scanningImages) {
            String s3Key = image.getS3Key();
            
            try {
                // Check if image exists in permanent bucket (scan passed)
                if (s3Service.fileExists(permanentBucket, s3Key)) {
                    image.setStatus(ImageStatus.CLEAN);
                    imageRepository.save(image);
                    log.info("Image marked as CLEAN: {}", s3Key);
                    continue;
                }
                
                // Check if image still exists in quarantine (scan pending or user hasn't uploaded)
                if (s3Service.fileExists(quarantineBucket, s3Key)) {
                    // Check if it's been too long (scan might be stuck)
                    Duration scanDuration = Duration.between(image.getUploadDate(), LocalDateTime.now());
                    
                    if (scanDuration.toMinutes() > SCAN_TIMEOUT_MINUTES) {
                        log.warn("Image stuck in SCANNING for {} minutes: {}", 
                            scanDuration.toMinutes(), s3Key);
                        // Could mark as failed or trigger manual review
                        // For now, just log it
                    } else {
                        log.debug("Image still in quarantine, scan pending: {} (age: {} min)", 
                            s3Key, scanDuration.toMinutes());
                    }
                    continue;
                }
                
                // Image doesn't exist in either bucket
                // This could mean:
                // 1. User got presigned URL but never uploaded (most common)
                // 2. Lambda deleted it as malware
                // 3. Manual deletion
                
                Duration timeSinceCreation = Duration.between(image.getUploadDate(), LocalDateTime.now());
                
                // Only mark as THREAT_DETECTED if enough time has passed
                // Otherwise, user might just not have uploaded yet
                if (timeSinceCreation.toMinutes() > SCAN_TIMEOUT_MINUTES) {
                    image.setStatus(ImageStatus.THREAT_DETECTED);
                    imageRepository.save(image);
                    log.warn("Image marked as THREAT_DETECTED (file not found, {} min old): {}", 
                        timeSinceCreation.toMinutes(), s3Key);
                } else {
                    log.debug("Image record exists but file not uploaded yet: {} (age: {} min)", 
                        s3Key, timeSinceCreation.toMinutes());
                }
                
            } catch (Exception e) {
                log.error("Error checking status for image: {}", s3Key, e);
            }
        }
        
        log.info("Image scan status sync completed");
    }
}