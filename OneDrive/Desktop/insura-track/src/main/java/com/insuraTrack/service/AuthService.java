package com.insuraTrack.service;

import com.insuraTrack.dto.*;
import com.insuraTrack.enums.Role;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.UserRepository;
import com.insuraTrack.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${app.admin.secret:admin123}")
    private String adminSecret;

    // 🔐 LOGIN
    public AuthResponse login(AuthRequest request) {

        // ✅ Step 1: Check if email exists — specific message
        // FIXED: Use findByEmailAndDeletedFalse instead of findByEmail
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        // ✅ Step 2: Check account active status before auth attempt
        if (!user.isActive()) {
            throw new RuntimeException("Your account has been disabled. Please contact the administrator.");
        }

        // ✅ Step 3: Authenticate and catch Spring Security exceptions with proper messages
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Incorrect password. Please try again.");
        } catch (DisabledException e) {
            throw new RuntimeException("Your account has been disabled. Please contact the administrator.");
        } catch (LockedException e) {
            throw new RuntimeException("Your account is locked. Please contact the administrator.");
        } catch (Exception e) {
            throw new RuntimeException("Login failed. Please try again.");
        }

        // ✅ Step 4: Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // 🔐 REGISTER
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new RuntimeException("This email is already registered. Please login instead.");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters.");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Name is required.");
        }

        Role role;
        if (request.getAdminKey() != null && !request.getAdminKey().isEmpty()
                && adminSecret.equals(request.getAdminKey())) {
            role = Role.ADMIN;
        } else {
            role = Role.USER;
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // 🔐 LOGOUT
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // Token blacklisting — implement with Redis if needed
    }
}