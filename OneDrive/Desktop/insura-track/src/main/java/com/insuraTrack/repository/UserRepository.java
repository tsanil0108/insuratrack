package com.insuraTrack.repository;

import com.insuraTrack.enums.Role;
import com.insuraTrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    List<User> findAllByDeletedFalse();

    boolean existsByEmail(String email);

    // ✅ Role is an enum — pass Role.ADMIN, not the string "ADMIN"
    List<User> findByRoleAndDeletedFalse(Role role);

    @Query("SELECT u FROM User u WHERE u.deleted = true")
    List<User> findAllByDeletedTrue();
}