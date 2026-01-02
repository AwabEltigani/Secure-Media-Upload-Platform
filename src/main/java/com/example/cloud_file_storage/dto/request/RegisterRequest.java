package com.example.cloud_file_storage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//values the user need to enter when creating a user 
public record RegisterRequest(

    // Ensures the username isn't empty and meets specific length/character requirements
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, hyphens and underscores")
    String username,

    //Added to capture the user's first name from the request
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be under 50 characters")
    String firstName,

    //Added to capture the user's last name from the request
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be under 50 characters")
    String lastName,

    // Standard email validation using Jakarta constraints
    @Email(message = "must be a valid email")
    @NotBlank(message = "Email is required")
    String email,

    // Enforces password security: length and complexity (Upper, Lower, and Digit) prevents sql injection
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
        message = "Password must contain at least one uppercase letter, one lowercase and one number")
    String password
) {}