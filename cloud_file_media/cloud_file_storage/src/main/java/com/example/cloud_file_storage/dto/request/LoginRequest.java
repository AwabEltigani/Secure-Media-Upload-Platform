package com.example.cloud_file_storage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
Record generates the getters,toString,equals and hashcode methods
*/
public record LoginRequest(
    @Email(message="enter an email")
    @NotBlank(message = "Email is required")
    String email,
    @NotBlank(message = "password is required")
    String password
) {}
