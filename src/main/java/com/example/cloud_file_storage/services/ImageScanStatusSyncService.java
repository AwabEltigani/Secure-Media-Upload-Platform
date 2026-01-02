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
    
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void syncImageStatus() {
        log.info("Starting image scan status sync");
        
        List<Image> scanningImages = imageRepository.findByStatus(ImageStatus.SCANNING);
        log.info("Found {} images in SCANNING status", scanningImages.size());
        
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
                
                // Check if image still exists in quarantine (scan pending)
                if (s3Service.fileExists(quarantineBucket, s3Key)) {
                    log.debug("Image still in quarantine, scan pending: {}", s3Key);
                    continue;
                }
                
                // Image exists in neither bucket - must have been infected and deleted
                image.setStatus(ImageStatus.THREAT_DETECTED);
                imageRepository.save(image);
                log.warn("Image marked as THREAT_DETECTED (deleted by Lambda): {}", s3Key);
                
            } catch (Exception e) {
                log.error("Error checking status for image: {}", s3Key, e);
            }
        }
        
        log.info("Image scan status sync completed");
    }
}