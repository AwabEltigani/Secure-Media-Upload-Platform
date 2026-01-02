package com.example.cloud_file_storage.dto.response;

import java.time.LocalDateTime;
import com.example.cloud_file_storage.enums.ImageStatus; 
import com.example.cloud_file_storage.models.Image;    
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON
public record ImageResponse(
    Long id,
    String filename,
    Long fileSize,
    String contentType,
    ImageStatus status,
    
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime uploadDate, 

    String downloadUrl,
    String thumbnailUrl  // Fixed typo: thumbNailUrl -> thumbnailUrl
) 
{
    /**
     * Maps the Image entity to an ImageResponse DTO.
     */
    public static ImageResponse from(Image image) {
        return new ImageResponse(
            image.getId(),
            image.getFilename(),
            image.getFileSize(),
            image.getContentType(),
            image.getStatus(),
            image.getUploadDate(),
            null,
            null
        );
    }

    public ImageResponse withDownloadUrl(String url) {
        return new ImageResponse(
            id, 
            filename, 
            fileSize, 
            contentType, 
            status, 
            uploadDate, 
            url, 
            thumbnailUrl
        );
    }

    public ImageResponse withThumbnailUrl(String thumbUrl) {
        return new ImageResponse(
            id, 
            filename, 
            fileSize, 
            contentType, 
            status, 
            uploadDate, 
            downloadUrl, 
            thumbUrl
        );
    }
    
    /**
     * Convenience method to add both URLs at once
     */
    public ImageResponse withUrls(String downloadUrl, String thumbnailUrl) {
        return new ImageResponse(
            id, 
            filename, 
            fileSize, 
            contentType, 
            status, 
            uploadDate, 
            downloadUrl, 
            thumbnailUrl
        );
    }
}