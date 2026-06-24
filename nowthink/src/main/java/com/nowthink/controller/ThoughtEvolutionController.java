package com.nowthink.controller;

import com.nowthink.model.ThoughtEvolution;
import com.nowthink.service.ThoughtEvolutionEngine;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evolution")
@CrossOrigin(origins = "*")
public class ThoughtEvolutionController {

    private final ThoughtEvolutionEngine evolutionEngine;

    public ThoughtEvolutionController(ThoughtEvolutionEngine evolutionEngine) {
        this.evolutionEngine = evolutionEngine;
    }

    @GetMapping
    public List<ThoughtEvolution> getAllEvolutions() {
        return evolutionEngine.getAllEvolutions();
    }

    @GetMapping("/{theme}")
    public List<ThoughtEvolution> getByTheme(@PathVariable String theme) {
        return evolutionEngine.getEvolutionByTheme(theme);
    }

    @GetMapping("/themes")
    public List<String> getAvailableThemes() {
        return List.of("confidence", "focus", "relationships",
                "identity", "productivity", "fear", "growth", "purpose");
    }
}