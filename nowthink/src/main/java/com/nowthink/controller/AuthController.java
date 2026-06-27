package com.nowthink.controller;

import com.nowthink.model.User;
import com.nowthink.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/success")
    public ResponseEntity<?> loginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String googleId = principal.getAttribute("sub");
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        User user = userRepository.findByGoogleId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPicture(picture);
            return userRepository.save(newUser);
        });

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "picture", user.getPicture() != null ? user.getPicture() : ""
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String googleId = principal.getAttribute("sub");
        return userRepository.findByGoogleId(googleId)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "picture", user.getPicture() != null ? user.getPicture() : ""
                )))
                .orElse(ResponseEntity.status(404).build());
    }

    @GetMapping("/logout-success")
    public ResponseEntity<?> logoutSuccess() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/failure")
    public ResponseEntity<?> loginFailure() {
        return ResponseEntity.status(401).body(Map.of("error", "Login failed"));
    }
}