package com.nowthink.controller;

import com.nowthink.config.NowthinkUserPrincipal;
import com.nowthink.model.User;
import com.nowthink.repository.UserRepository;
import com.nowthink.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/oauth-success")
    public void oauthSuccess(@AuthenticationPrincipal OAuth2User principal,
                             jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {

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

        String token = jwtService.generateToken(googleId, email, name);
        response.sendRedirect("https://nowthink-frontend.vercel.app?token=" + token);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return userRepository.findByGoogleId(principal.getUserId())
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "picture", user.getPicture() != null ? user.getPicture() : ""
                )))
                .orElse(ResponseEntity.status(404).build());
    }
}