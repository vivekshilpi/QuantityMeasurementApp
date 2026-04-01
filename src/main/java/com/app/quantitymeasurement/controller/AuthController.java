package com.app.quantitymeasurement.controller;

import com.app.quantitymeasurement.model.User;
import com.app.quantitymeasurement.repository.UserRepository;
import com.app.quantitymeasurement.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    // ==========================================
    // GOOGLE LOGIN (Frontend sends ID token)
    // ==========================================
    @PostMapping("/google-login")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody GoogleLoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Verify the Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                     .build();

            GoogleIdToken idToken = verifier.verify(request.getToken());

            if (idToken == null) {
                response.put("message", "Invalid Google token");
                return ResponseEntity.badRequest().body(response);
            }

            // Extract user info from token
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String googleId = payload.getSubject();

            // Find or create user
            User user = userRepository.findByEmail(email)
                    .map(existingUser -> {
                        existingUser.setName(name);
                        existingUser.setPicture(picture);
                        if (existingUser.getProviderId() == null) {
                            existingUser.setProviderId(googleId);
                        }
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email(email)
                            .name(name)
                            .picture(picture)
                            .provider("GOOGLE")
                            .providerId(googleId)
                            .role(User.Role.USER)
                            .build()));

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getName());

            response.put("token", token);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "fullName", user.getName() != null ? user.getName() : "",
                    "picture", user.getPicture() != null ? user.getPicture() : ""
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", "Google authentication failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==========================================
    // EMAIL/PASSWORD LOGIN
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            response.put("message", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if user has a password (not Google-only user)
        if (user.getPassword() == null) {
            response.put("message", "This account uses Google Sign-In. Please login with Google.");
            return ResponseEntity.badRequest().body(response);
        }

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("message", "Invalid password");
            return ResponseEntity.badRequest().body(response);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getName());

        response.put("token", token);
        response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getName() != null ? user.getName() : "",
                "picture", user.getPicture() != null ? user.getPicture() : ""
        ));

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // REGISTER (Email/Password)
    // ==========================================
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            response.put("message", "Email already exists");
            return ResponseEntity.badRequest().body(response);
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .role(User.Role.USER)
                .build();
        user = userRepository.save(user);

        // Generate token
        String token = jwtUtil.generateToken(user.getEmail(), user.getName());

        response.put("token", token);
        response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getName() != null ? user.getName() : ""
        ));

        return ResponseEntity.ok(response);
    }

    // OAuth2 success callback - returns the JWT token
    @GetMapping("/oauth2/success")
    public ResponseEntity<Map<String, String>> oauth2Success(@RequestParam String token) {
        return ResponseEntity.ok(Map.of(
                "message", "Authentication successful",
                "token", token
        ));
    }

    // Get current authenticated user details
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Health check for auth service
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "Auth service is running"));
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public static class GoogleLoginRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String fullName;
        private String email;
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}