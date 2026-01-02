package com.example.cloud_file_storage.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import com.example.cloud_file_storage.enums.Role;
import com.example.cloud_file_storage.models.Users;

public record UserResponse(

    Long id,

    String username,

    String email,

    Role role,

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {

    //no password to not expose it to the user
    public static UserResponse from (Users user){
        if (user == null) {
        throw new IllegalArgumentException("User cannot be null");
    }
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
