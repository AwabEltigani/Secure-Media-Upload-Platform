package com.example.cloud_file_storage.services;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cloud_file_storage.dto.request.LoginRequest;
import com.example.cloud_file_storage.dto.request.RegisterRequest;
import com.example.cloud_file_storage.dto.response.AuthResponse;
import com.example.cloud_file_storage.enums.Role;
import com.example.cloud_file_storage.exceptions.ResourceNotFoundException;
import com.example.cloud_file_storage.exceptions.UserAlreadyExistsException;
import com.example.cloud_file_storage.models.Users;
import com.example.cloud_file_storage.repos.UsersRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Builder
/*
    AuthService handles user creation and user login 
    we are using JWT auth for secure login
*/
public class AuthService {

    private final UsersRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jWTservice;
    private final AuthenticationManager authenticationManager;

    /**
     * Register new user
     */
    public AuthResponse register(RegisterRequest request) {
        // Logs the start of the registration process for debugging
        log.info("Registering new user: {}", request.username());

        // Check if username already exists in the database to prevent duplicates
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(
                "Username already taken: " + request.username()
            );
        }

        // Check if email already exists in the database to prevent duplicates
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                "Email already registered: " + request.email()
            );
        }

        // Create new user entity using the Lombok Builder pattern
        // Added firstName and lastName to satisfy the database NOT NULL constraints
        Users user = Users.builder()
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName()) // Maps firstName from DTO to Entity
                .lastName(request.lastName())   // Maps lastName from DTO to Entity
                .password(passwordEncoder.encode(request.password())) // Hashes the password
                .role(Role.USER) // Assigns the default role
                .build();
        Users savedUser;

        //if user not found throw a illegal argument exception
        if (user != null) {
            savedUser = userRepository.save(user);
            } else {
            throw new IllegalArgumentException("User cannot be null");
            }
            savedUser = userRepository.save(user);

        // Generate a JWT token for the newly registered user so they are logged in immediately
        String token = jWTservice.generateToken(savedUser);

        log.info("Successfully registered user: {}", savedUser.getUsername());

        // Return the response object containing the token and user details
        return new AuthResponse(
            token,
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRole()
        );
    }

    /**
     * Login user
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.email());

        // Uses Spring Security's AuthenticationManager to verify credentials
        // If password/username is wrong, this method throws an AuthenticationException
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
            )
        );

        // Secondary check to ensure the user actually exists in our repository
        if(!userRepository.existsByEmail(request.email()) ){
            throw new UsernameNotFoundException(request.email() + ": was not found");
        }

        // Retrieve the full user entity from the database
        Users user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new ResourceNotFoundException("email not found"));
        
        // Generate a new JWT token for the session
        String token = jWTservice.generateToken(user);

        log.info("Successfully authenticated user: {}", user.getUsername());

        // Return the response object with the session token
        return new AuthResponse(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        );
    }
}