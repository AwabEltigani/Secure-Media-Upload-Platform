package com.example.cloud_file_storage.dto.request;

import jakarta.validation.constraints.*;

public record UpdateUserRequest(
    
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, hyphens, and underscores")
    String username,
    
    @Email(message = "Email must be valid")
    String email,
    
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password
) {}