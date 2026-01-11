package com.example.cloud_file_storage.repos;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.cloud_file_storage.models.Image;
import com.example.cloud_file_storage.enums.ImageStatus;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    List<Image> findByUserId(Long userId);
    
    Optional<Image> findByS3Key(String s3Key);
    
    List<Image> findByUserIdAndStatus(Long userId, ImageStatus status);
    
    List<Image> findByStatus(ImageStatus status);
    
    long countByUserIdAndStatus(Long userId, ImageStatus status);
    
    boolean existsByIdAndUserId(Long imageId, Long userId);

    
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Image i WHERE i.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    List<Image> findByUserIdAndContentTypeContaining(Long userId, String contentType);
    
    List<Image> findByStatusOrderByUploadDateAsc(ImageStatus status);
    
    List<Image> findTop10ByUserIdOrderByUploadDateDesc(Long userId);
    
    @Query("SELECT i FROM Image i WHERE i.uploadDate < :date AND i.status = :status")
    List<Image> findOldImagesByStatus(@Param("date") LocalDateTime date, @Param("status") ImageStatus status);
}