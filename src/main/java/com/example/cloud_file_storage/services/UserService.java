package com.example.cloud_file_storage.services;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cloud_file_storage.exceptions.ResourceNotFoundException;
import com.example.cloud_file_storage.exceptions.UserAlreadyExistsException;
import com.example.cloud_file_storage.models.Users;
import com.example.cloud_file_storage.repos.UsersRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;  

    @Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    log.debug("Loading user by email: {}", email);
    
    return usersRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
}

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Users getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        
        return usersRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public Users getUserByUsername(String username) {
        if(existsByUsername(username)){
            throw new UsernameNotFoundException(username+ " was not found");
        }
        log.debug("Fetching user by username: {}", username);
        
        return usersRepository.findByUsername(username);
    }

    /**
     * Get all users 
     */
    @Transactional(readOnly = true)
    public List<Users> getAllUsers() {
        log.debug("Fetching all users");
        return usersRepository.findAll();
    }

    /**
     * Update user
     */
    public Users updateUser(Long id, Users updatedUser) {
        log.info("Updating user with ID: {}", id);
        
        // Get existing user
        Users currentUser = usersRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        
        // Check if new username is taken by another user
        if (!currentUser.getUsername().equals(updatedUser.getUsername()) &&
            usersRepository.existsByUsername(updatedUser.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + updatedUser.getUsername());
        }
        
        // Check if new email is taken by another user
        if (!currentUser.getEmail().equals(updatedUser.getEmail()) &&
            usersRepository.existsByEmail(updatedUser.getEmail())) {
            throw new UserAlreadyExistsException("Email already taken: " + updatedUser.getEmail());
        }
        
        // Update username and email
        currentUser.setUsername(updatedUser.getUsername());
        currentUser.setEmail(updatedUser.getEmail());
        
        // Update password only if provided
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            currentUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        
        // Save updated user
        Users savedUser = usersRepository.save(currentUser);
        
        log.info("Successfully updated user: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Delete user
     */
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        
        Users user = getUserById(id);
        usersRepository.delete(user);
        
        log.info("Successfully deleted user: {}", user.getUsername());
    }

    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return usersRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usersRepository.existsByEmail(email);
    }

    public Users getUserByEmail(String email){
        return usersRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("Email was not found"));
    }
}