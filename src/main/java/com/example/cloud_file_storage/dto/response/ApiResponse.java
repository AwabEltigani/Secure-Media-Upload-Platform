package com.example.cloud_file_storage.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp,
    
    boolean success,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
            LocalDateTime.now(),
            true,
            message,
            data
        );
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return success("Success", data);
    }

    // Fixed error method
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
            LocalDateTime.now(),
            false,
            message,
            null
        );
    }
    
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(
            LocalDateTime.now(),
            false,
            message,
            data
        );
    }
}