package com.nowthink.controller;

import com.nowthink.model.Observation;
import com.nowthink.repository.ObservationRepository;
import com.nowthink.service.DiscoveryEngine;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observe")
@CrossOrigin(origins = "*")
public class ObservationController {

    private final ObservationRepository observationRepository;
    private final ChatClient extractorClient;
    private final ChatClient energyClient;

    public ObservationController(ObservationRepository observationRepository,
                                 OpenAiChatModel model) {
        this.observationRepository = observationRepository;

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
                theme = extractorClient.prompt().user(rawText).call().content().trim();
            } catch (Exception e) {
                // keep default theme
            }

            int energy = 5;
            try {
                String raw = energyClient.prompt().user(rawText).call().content().trim();
                energy = Integer.parseInt(raw.replaceAll("[^0-9]", "").substring(0, 1));
                energy = Math.min(10, Math.max(1, energy));
            } catch (Exception e) {
                // keep default energy
            }

            Observation obs = new Observation();
            obs.setRawText(rawText.trim());
            obs.setExtractedTheme(theme);
            obs.setEnergyScore(energy);
            observationRepository.save(obs);

            long count = observationRepository.count();

            return ResponseEntity.ok(Map.of(
                    "id", obs.getId(),
                    "theme", theme,
                    "energyScore", energy,
                    "totalObservations", count,
                    "readyForDiscovery", count >= 3
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping
    public List<Observation> getAllObservations() {
        return observationRepository.findAllByOrderByCreatedAtDesc();
    }
}
