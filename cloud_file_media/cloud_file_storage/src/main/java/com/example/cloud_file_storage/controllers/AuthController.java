package com.example.cloud_file_storage.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.cloud_file_storage.dto.request.LoginRequest;
import com.example.cloud_file_storage.dto.request.RegisterRequest;
import com.example.cloud_file_storage.dto.response.AuthResponse;
import com.example.cloud_file_storage.dto.response.UserResponse;
import com.example.cloud_file_storage.exceptions.UnauthorizedException;
import com.example.cloud_file_storage.dto.response.ApiResponse;
import com.example.cloud_file_storage.models.Users;
import com.example.cloud_file_storage.services.AuthService;
import com.example.cloud_file_storage.services.TokenBlacklistService;
import com.example.cloud_file_storage.services.UserService;





@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Register new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        
        log.info("Registration request for username: {}", request.username());
        
        AuthResponse response = authService.register(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Login user
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("Login request for username: {}", request.email());
        
        AuthResponse response = authService.login(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user
     * POST /api/auth/logout
     * using a delete token after log out in the prod use redis
     */
@PostMapping("/logout")
public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String jwt = authHeader.substring(7);
        tokenBlacklistService.blacklistToken(jwt); 
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
}

    /**
     * Get current user info
     * GET /api/auth/me
     */
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Users user = userService.getUserByEmail(userDetails.getUsername());
    return ResponseEntity.ok(UserResponse.from(user));
}


}