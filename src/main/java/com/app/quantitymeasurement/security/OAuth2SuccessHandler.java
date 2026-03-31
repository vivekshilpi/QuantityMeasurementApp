package com.app.quantitymeasurement.security;

import com.app.quantitymeasurement.model.User;
import com.app.quantitymeasurement.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful OAuth2 authentication - creates/updates user and generates JWT.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:8080/api/v1/auth/oauth2/success}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // Create or update user
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setName(name);
                    existingUser.setPicture(picture);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(name)
                        .picture(picture)
                        .provider("GOOGLE")
                        .providerId(oAuth2User.getAttribute("sub"))
                        .role(User.Role.USER)
                        .build()));

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getName());

        // Redirect with token
        response.sendRedirect(redirectUri + "?token=" + token);
    }
}