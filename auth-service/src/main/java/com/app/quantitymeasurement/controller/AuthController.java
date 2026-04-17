package com.app.quantitymeasurement.controller;

import com.app.quantitymeasurement.model.User;
import com.app.quantitymeasurement.repository.UserRepository;
import com.app.quantitymeasurement.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @PostMapping("/google-login")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody GoogleLoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String googleToken = request.resolveToken();
            if (googleToken == null || googleToken.isBlank()) {
                response.put("message", "Google token is missing");
                return ResponseEntity.badRequest().body(response);
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                response.put("message", "Invalid Google token");
                return ResponseEntity.badRequest().body(response);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String googleId = payload.getSubject();

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

            response.put("token", jwtUtil.generateToken(user.getEmail(), user.getName()));
            response.put("user", buildUserPayload(user));
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            response.put("message", "Google authentication failed: " + exception.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            response.put("message", "User not found");
            return ResponseEntity.badRequest().body(response);
        }
        if (user.getPassword() == null) {
            response.put("message", "This account uses Google Sign-In. Please login with Google.");
            return ResponseEntity.badRequest().body(response);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("message", "Invalid password");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("token", jwtUtil.generateToken(user.getEmail(), user.getName()));
        response.put("user", buildUserPayload(user));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByEmail(request.getEmail())) {
            response.put("message", "Email already exists");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .name(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .role(User.Role.USER)
                .build());

        response.put("token", jwtUtil.generateToken(user.getEmail(), user.getName()));
        response.put("user", buildUserPayload(user));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<Map<String, String>> oauth2Success(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("message", "Authentication successful", "token", token));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "Auth service is running"));
    }

    private Map<String, Object> buildUserPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId());
        payload.put("email", user.getEmail());
        payload.put("fullName", user.getName() == null ? "" : user.getName());
        payload.put("picture", user.getPicture() == null ? "" : user.getPicture());
        return payload;
    }

    public static class GoogleLoginRequest {
        private String token;
        private String credential;
        private String idToken;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public String resolveToken() {
            if (token != null && !token.isBlank()) {
                return token;
            }
            if (credential != null && !credential.isBlank()) {
                return credential;
            }
            return idToken;
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RegisterRequest {
        private String fullName;
        private String email;
        private String password;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
