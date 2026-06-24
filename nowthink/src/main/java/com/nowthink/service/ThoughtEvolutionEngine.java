package com.nowthink.service;

import com.nowthink.model.Observation;
import com.nowthink.model.ThoughtEvolution;
import com.nowthink.repository.ThoughtEvolutionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ThoughtEvolutionEngine {

    private static final Logger log = LoggerFactory.getLogger(ThoughtEvolutionEngine.class);

    private final ThoughtEvolutionRepository evolutionRepository;
    private final ChatClient beliefExtractor;
    private final ChatClient themeClassifier;

    public ThoughtEvolutionEngine(ThoughtEvolutionRepository evolutionRepository,
                                  OpenAiChatModel model) {
        this.evolutionRepository = evolutionRepository;

        this.beliefExtractor = ChatClient.builder(model)
                .defaultSystem("""
                You are a belief extractor.
                Given a person's observation, extract the core belief or feeling
                they are expressing in ONE short sentence.
                Start with "I" — first person.
                Maximum 10 words.
                Return ONLY the belief sentence. Nothing else.
                Examples:
                - "I am afraid of failing before I start"
                - "I feel capable when I focus"
                - "I doubt my own decisions constantly"
                """)
                .build();

        this.themeClassifier = ChatClient.builder(model)
                .defaultSystem("""
                You are a theme classifier.
                Given a belief sentence, classify it into ONE of these themes:
                confidence, focus, relationships, identity, productivity, fear, growth, purpose
                Return ONLY the theme word. Nothing else.
                """)
                .build();
    }

    public void extractAndStore(Observation observation) {
        try {
            String belief = beliefExtractor.prompt()
                    .user(observation.getRawText())
                    .call()
                    .content()
                    .trim();

            log.info("Extracted belief: {}", belief);

            String theme = themeClassifier.prompt()
                    .user(belief)
                    .call()
                    .content()
                    .trim()
                    .toLowerCase();

            log.info("Classified theme: {}", theme);

            ThoughtEvolution evolution = new ThoughtEvolution();
            evolution.setBelief(belief);
            evolution.setTheme(theme);
            evolution.setSourceObservation(observation.getRawText());
            evolutionRepository.save(evolution);

        } catch (Exception e) {
            log.error("ThoughtEvolutionEngine failed: {}", e.getMessage());
        }
    }

    public List<ThoughtEvolution> getEvolutionByTheme(String theme) {
        return evolutionRepository.findByThemeOrderByRecordedAtAsc(theme);
    }

    public List<ThoughtEvolution> getAllEvolutions() {
        return evolutionRepository.findAllByOrderByRecordedAtDesc();
    }
}