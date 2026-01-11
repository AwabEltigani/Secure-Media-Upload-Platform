package com.example.cloud_file_storage.dto.request;

import jakarta.validation.constraints.*;

/**
 * DTO for the initial image upload metadata.
 * Status is not included here because the service defaults it to SCANNING.
 */
public record SaveImageRequest(

    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename is too long")
    String fileName,

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = 1073741824L, message = "File size must not exceed 1 GB")
    Long fileSize,

    @NotBlank(message = "Content type is required")
    @Pattern(
        regexp = "^image/(jpeg|png|gif|webp|bmp)$",
        message = "Invalid image content type. Only JPEG, PNG, GIF, WEBP, and BMP are allowed."
    )
    String contentType,

    @NotBlank(message = "S3 key is required")
    @Pattern(
        regexp = "^users/[a-zA-Z0-9._@-]+/.*$",
        message = "S3 key must follow the pattern: users/<username>/<filename>"
    )
    String s3Key
) {
}