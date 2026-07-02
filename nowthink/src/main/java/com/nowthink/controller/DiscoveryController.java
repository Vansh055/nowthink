package com.nowthink.controller;

import com.nowthink.config.NowthinkUserPrincipal;
import com.nowthink.model.Discovery;
import com.nowthink.repository.DiscoveryRepository;
import com.nowthink.service.DiscoveryEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<?> generateDiscovery(@AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(discoveryEngine.generateDiscovery(principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<?> getAllDiscoveries(@AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(discoveryEngine.getAllDiscoveries(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDiscovery(@PathVariable Long id,
                                          @AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return discoveryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}