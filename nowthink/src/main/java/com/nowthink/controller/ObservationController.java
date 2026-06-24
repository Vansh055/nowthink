package com.nowthink.controller;

import com.nowthink.model.Observation;
import com.nowthink.repository.ObservationRepository;
import com.nowthink.service.ContradictionEngine;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observe")
@CrossOrigin(origins = "*")
public class ObservationController {

    private static final Logger log = LoggerFactory.getLogger(ObservationController.class);

    private final ObservationRepository observationRepository;
    private final ContradictionEngine contradictionEngine;
    private final ChatClient extractorClient;
    private final ChatClient energyClient;

    public ObservationController(ObservationRepository observationRepository,
                                 ContradictionEngine contradictionEngine,
                                 OpenAiChatModel model) {
        this.observationRepository = observationRepository;
        this.contradictionEngine = contradictionEngine;

        this.extractorClient = ChatClient.builder(model)
                .defaultSystem("""
                You are an observation extractor.
                Given a person's raw observation, extract the core theme in 5 words or less.
                Return ONLY the theme. No explanation. No punctuation at the end.
                Example outputs: "Fear of starting" or "Felt proud after finishing"
                """)
                .build();

        this.energyClient = ChatClient.builder(model)
                .defaultSystem("""
                Score the energy level in this observation from 1 to 10.
                1 = completely drained. 10 = highly energized.
                Return ONLY a single digit or two digit number. Nothing else. No words.
                """)
                .build();
    }

    @PostMapping
    public ResponseEntity<?> addObservation(@RequestBody String rawText) {
        try {
            if (rawText == null || rawText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Observation cannot be empty"));
            }

            String theme = "Unnamed observation";
            try {
                log.info("Calling extractor AI for: {}", rawText.substring(0, Math.min(50, rawText.length())));
                String result = extractorClient.prompt().user(rawText).call().content().trim();
                log.info("Extractor returned: {}", result);
                if (result != null && !result.isEmpty()) {
                    theme = result;
                }
            } catch (Exception e) {
                log.error("Extractor AI failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            }

            int energy = 5;
            try {
                log.info("Calling energy scorer AI");
                String raw = energyClient.prompt().user(rawText).call().content().trim();
                log.info("Energy scorer returned: {}", raw);
                String digits = raw.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    energy = Integer.parseInt(digits.substring(0, Math.min(2, digits.length())));
                    energy = Math.min(10, Math.max(1, energy));
                }
            } catch (Exception e) {
                log.error("Energy AI failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            }

            Observation obs = new Observation();
            obs.setRawText(rawText.trim());
            obs.setExtractedTheme(theme);
            obs.setEnergyScore(energy);
            observationRepository.save(obs);

            // Run contradiction check asynchronously so it doesn't block the response
            final Observation savedObs = obs;
            new Thread(() -> {
                try {
                    contradictionEngine.checkAndUpdate(savedObs);
                } catch (Exception e) {
                    log.error("Contradiction engine error: {}", e.getMessage());
                }
            }).start();

            long count = observationRepository.count();

            return ResponseEntity.ok(Map.of(
                    "id", obs.getId(),
                    "theme", theme,
                    "energyScore", energy,
                    "totalObservations", count,
                    "readyForDiscovery", count >= 3
            ));
        } catch (Exception e) {
            log.error("Full observation error: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping
    public List<Observation> getAllObservations() {
        return observationRepository.findAllByOrderByCreatedAtDesc();
    }
}