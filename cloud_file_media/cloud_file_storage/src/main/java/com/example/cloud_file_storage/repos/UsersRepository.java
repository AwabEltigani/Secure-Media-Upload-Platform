package com.example.cloud_file_storage.repos;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.cloud_file_storage.models.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> { 
    
    boolean existsByEmail(String email);
    
    Optional<Users> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsById(Long id);

    Users findByUsername(String username);

    Users getUserByUsername(String username);

    List<Users> findAll();

    Optional<Users> findById(Long id);


}