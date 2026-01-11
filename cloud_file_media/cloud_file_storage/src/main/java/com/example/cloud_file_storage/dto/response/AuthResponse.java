package com.example.cloud_file_storage.dto.response;

import com.example.cloud_file_storage.enums.Role;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String username,    
    String email,
    Role role
) {
    public AuthResponse(String token, Long userId, String username, String email, Role role) {
        this(token, "Bearer", userId, username, email, role);
    }
}