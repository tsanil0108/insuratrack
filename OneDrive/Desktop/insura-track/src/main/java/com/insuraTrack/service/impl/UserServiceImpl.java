package com.insuraTrack.service.impl;

import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.UserRepository;
import com.insuraTrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByDeletedFalse();
    }

    @Override
    @Transactional
    public User updateUser(String id, User updatedUser) {
        User existing = getUserById(id);
        if (updatedUser.getName() != null && !updatedUser.getName().isBlank()) {
            existing.setName(updatedUser.getName());
        }
        if (updatedUser.getRole() != null) {
            existing.setRole(updatedUser.getRole());
        }
        existing.setActive(updatedUser.isActive());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDeleteUser(String id, String deletedByEmail) {
        User user = getUserById(id);
        user.softDelete(deletedByEmail);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(String id) {
        User user = getUserById(id);
        user.restore();
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * FIX: toggles active flag for /toggle-active endpoint.
     * If user is active → deactivate. If inactive → activate.
     */
    @Override
    @Transactional
    public void toggleActive(String id) {
        User user = getUserById(id);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}