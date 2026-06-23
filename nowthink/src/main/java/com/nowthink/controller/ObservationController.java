package com.nowthink.controller;

import com.nowthink.model.Observation;
import com.nowthink.repository.ObservationRepository;
import com.nowthink.service.DiscoveryEngine;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observe")
@CrossOrigin(origins = "*")
public class ObservationController {

    private final ObservationRepository observationRepository;
    private final DiscoveryEngine discoveryEngine;
    private final ChatClient extractorClient;
    private final ChatClient energyClient;

    public ObservationController(ObservationRepository observationRepository,
                                 DiscoveryEngine discoveryEngine,
                                 OpenAiChatModel model) {
        this.observationRepository = observationRepository;
        this.discoveryEngine = discoveryEngine;

        this.extractorClient = ChatClient.builder(model)
                .defaultSystem("""
                You are an observation extractor.
                Given a person's raw observation, extract the core theme in 5 words or less.
                Return ONLY the theme. No explanation.
                Example: "avoided starting the project" or "felt proud after finishing"
                """)
                .build();

        this.energyClient = ChatClient.builder(model)
                .defaultSystem("""
                Score the energy level in this observation from 1-10.
                1 = completely drained. 10 = highly energized.
                Return ONLY the number. Nothing else.
                """)
                .build();
    }

    @PostMapping
    public Map<String, Object> addObservation(@RequestBody String rawText) {
        String theme = extractorClient.prompt().user(rawText).call().content().trim();

        int energy = 5;
        try {
            String raw = energyClient.prompt().user(rawText).call().content().trim();
            energy = Integer.parseInt(raw.replaceAll("[^0-9]", ""));
            energy = Math.min(10, Math.max(1, energy));
        } catch (Exception ignored) {}

        Observation obs = new Observation();
        obs.setRawText(rawText);
        obs.setExtractedTheme(theme);
        obs.setEnergyScore(energy);
        observationRepository.save(obs);

        long count = observationRepository.count();

        return Map.of(
                "id", obs.getId(),
                "theme", theme,
                "energyScore", energy,
                "totalObservations", count,
                "readyForDiscovery", count >= 3
        );
    }

    @GetMapping
    public List<Observation> getAllObservations() {
        return observationRepository.findAllByOrderByCreatedAtDesc();
    }
}
