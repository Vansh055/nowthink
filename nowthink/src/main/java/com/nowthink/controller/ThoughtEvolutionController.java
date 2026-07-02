package com.nowthink.controller;

import com.nowthink.config.NowthinkUserPrincipal;
import com.nowthink.service.ThoughtEvolutionEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evolution")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ThoughtEvolutionController {

    private final ThoughtEvolutionEngine evolutionEngine;

    public ThoughtEvolutionController(ThoughtEvolutionEngine evolutionEngine) {
        this.evolutionEngine = evolutionEngine;
    }

    @GetMapping
    public ResponseEntity<?> getAllEvolutions(@AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(evolutionEngine.getAllEvolutions(principal.getUserId()));
    }

    @GetMapping("/{theme}")
    public ResponseEntity<?> getByTheme(@PathVariable String theme,
                                        @AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(evolutionEngine.getEvolutionByTheme(principal.getUserId(), theme));
    }

    @GetMapping("/themes")
    public List<String> getAvailableThemes() {
        return List.of("confidence", "focus", "relationships",
                "identity", "productivity", "fear", "growth", "purpose");
    }
}