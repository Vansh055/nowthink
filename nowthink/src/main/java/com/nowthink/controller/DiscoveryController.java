package com.nowthink.controller;

import com.nowthink.model.Discovery;
import com.nowthink.repository.DiscoveryRepository;
import com.nowthink.service.DiscoveryEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/discoveries")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class DiscoveryController {

    private final DiscoveryEngine discoveryEngine;
    private final DiscoveryRepository discoveryRepository;

    public DiscoveryController(DiscoveryEngine discoveryEngine,
                               DiscoveryRepository discoveryRepository) {
        this.discoveryEngine = discoveryEngine;
        this.discoveryRepository = discoveryRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateDiscovery(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String userId = principal.getAttribute("sub");
        return ResponseEntity.ok(discoveryEngine.generateDiscovery(userId));
    }

    @GetMapping
    public ResponseEntity<?> getAllDiscoveries(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String userId = principal.getAttribute("sub");
        return ResponseEntity.ok(discoveryEngine.getAllDiscoveries(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDiscovery(@PathVariable Long id,
                                          @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return discoveryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}