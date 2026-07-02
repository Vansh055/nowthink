package com.nowthink.controller;

import com.nowthink.config.NowthinkUserPrincipal;
import com.nowthink.model.Observation;
import com.nowthink.repository.ObservationRepository;
import com.nowthink.service.ContradictionEngine;
import com.nowthink.service.ThoughtEvolutionEngine;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observe")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ObservationController {

    private static final Logger log = LoggerFactory.getLogger(ObservationController.class);

    private final ObservationRepository observationRepository;
    private final ContradictionEngine contradictionEngine;
    private final ThoughtEvolutionEngine thoughtEvolutionEngine;
    private final ChatClient extractorClient;
    private final ChatClient energyClient;

    public ObservationController(ObservationRepository observationRepository,
                                 ContradictionEngine contradictionEngine,
                                 ThoughtEvolutionEngine thoughtEvolutionEngine,
                                 OpenAiChatModel model) {
        this.observationRepository = observationRepository;
        this.contradictionEngine = contradictionEngine;
        this.thoughtEvolutionEngine = thoughtEvolutionEngine;

        this.extractorClient = ChatClient.builder(model)
                .defaultSystem("""
                You are an observation extractor.
                Given a person's raw observation, extract the core theme in 5 words or less.
                Return ONLY the theme. No explanation. No punctuation at the end.
                """)
                .build();

        this.energyClient = ChatClient.builder(model)
                .defaultSystem("""
                Score the energy level in this observation from 1 to 10.
                Return ONLY a single digit or two digit number. Nothing else.
                """)
                .build();
    }

    @PostMapping
    public ResponseEntity<?> addObservation(@RequestBody String rawText,
                                            @AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String userId = principal.getUserId();

        try {
            if (rawText == null || rawText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty observation"));
            }

            String theme = "Unnamed observation";
            try {
                String result = extractorClient.prompt().user(rawText).call().content().trim();
                if (result != null && !result.isEmpty()) theme = result;
            } catch (Exception e) {
                log.error("Extractor failed: {}", e.getMessage());
            }

            int energy = 5;
            try {
                String raw = energyClient.prompt().user(rawText).call().content().trim();
                String digits = raw.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    energy = Integer.parseInt(digits.substring(0, Math.min(2, digits.length())));
                    energy = Math.min(10, Math.max(1, energy));
                }
            } catch (Exception e) {
                log.error("Energy scorer failed: {}", e.getMessage());
            }

            Observation obs = new Observation();
            obs.setUserId(userId);
            obs.setRawText(rawText.trim());
            obs.setExtractedTheme(theme);
            obs.setEnergyScore(energy);
            observationRepository.save(obs);

            final Observation savedObs = obs;
            new Thread(() -> {
                try { contradictionEngine.checkAndUpdate(savedObs, userId); }
                catch (Exception e) { log.error("Contradiction error: {}", e.getMessage()); }
                try { thoughtEvolutionEngine.extractAndStore(savedObs, userId); }
                catch (Exception e) { log.error("Evolution error: {}", e.getMessage()); }
            }).start();

            long count = observationRepository.countByUserId(userId);
            return ResponseEntity.ok(Map.of(
                    "id", obs.getId(),
                    "theme", theme,
                    "energyScore", energy,
                    "totalObservations", count,
                    "readyForDiscovery", count >= 3
            ));
        } catch (Exception e) {
            log.error("Observation error: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllObservations(@AuthenticationPrincipal NowthinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(observationRepository.findByUserIdOrderByCreatedAtDesc(principal.getUserId()));
    }
}