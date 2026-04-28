package com.insuraTrack.service;

import com.insuraTrack.model.User;

import java.util.List;

public interface UserService {

    User createUser(User user);

    User getUserById(String id);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUser(String id, User updatedUser);

    void softDeleteUser(String id, String deletedByEmail);

    void activateUser(String id);

    /** FIX: toggles active flag — used by /toggle-active endpoint */
    void toggleActive(String id);

    boolean existsByEmail(String email);
}