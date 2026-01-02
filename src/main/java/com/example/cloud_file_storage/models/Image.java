package com.example.cloud_file_storage.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


import com.example.cloud_file_storage.enums.ImageStatus;


@Entity
@Table(name = "images") 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {


    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;
    
    @Column(name = "filename", nullable = false) 
    private String filename;
    
    @Column(name = "file_size", nullable = false) 
    private Long fileSize;
    
    @Column(nullable = false)
    private String contentType;
    
    @Column(name = "s3_key", nullable = false, unique = true, length = 500) 
    private String s3Key;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageStatus status;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;
    
    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        if (status == null) {
            status = ImageStatus.SCANNING;
        }
    }
    
}