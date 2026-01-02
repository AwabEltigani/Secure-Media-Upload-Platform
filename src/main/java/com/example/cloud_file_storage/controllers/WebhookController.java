package com.example.cloud_file_storage.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.cloud_file_storage.dto.request.ScanResultRequest;
import com.example.cloud_file_storage.services.ImageService;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ImageService imageService;

    /**
     * Handle image scan results from Lambda webhook.
     * Updates image status and moves file if necessary.
     *
     * @param request Scan result payload
     * @return 200 OK
     */
    @PostMapping("/image-scan")
    public ResponseEntity<Void> handleScanResult(
            @Valid @RequestBody ScanResultRequest request) {

        log.info("Received scan result: s3Key={}, status={}", 
                request.s3Key(), request.status());

        try {
            
            imageService.handleScanResult(request.s3Key(), request.status());
        } catch (Exception e) {
            log.error("Failed to process scan result for s3Key={}", request.s3Key(), e);
            
            return ResponseEntity.status(500).build();
        }


        return ResponseEntity.ok().build();
    }
}
