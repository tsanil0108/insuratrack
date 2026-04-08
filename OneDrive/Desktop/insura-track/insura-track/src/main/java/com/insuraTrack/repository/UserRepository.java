package com.insuraTrack.repository;

import com.insuraTrack.enums.Role;
import com.insuraTrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // 🔥 IMPORTANT (for admin check)
    boolean existsByRole(Role role);
}