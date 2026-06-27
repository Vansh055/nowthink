package com.nowthink.service;

import com.nowthink.model.Discovery;
import com.nowthink.model.Observation;
import com.nowthink.repository.DiscoveryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ContradictionEngine {

    private static final Logger log = LoggerFactory.getLogger(ContradictionEngine.class);

    private final DiscoveryRepository discoveryRepository;
    private final ChatClient contradictionClient;

    public ContradictionEngine(DiscoveryRepository discoveryRepository,
                               OpenAiChatModel model) {
        this.discoveryRepository = discoveryRepository;

        this.contradictionClient = ChatClient.builder(model)
                .defaultSystem("""
                You are a contradiction detector.
                Given a hypothesis (claim) and a new observation, determine if the observation
                contradicts the hypothesis.
                Reply with ONLY one word:
                YES — if the observation clearly contradicts the hypothesis
                NO — if it does not contradict
                No explanation. No punctuation. Just YES or NO.
                """)
                .build();
    }

    public void checkAndUpdate(Observation newObservation, String userId) {
        List<Discovery> supportedDiscoveries = discoveryRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(d -> "Supported".equals(d.getStatus()))
                .toList();

        for (Discovery discovery : supportedDiscoveries) {
            try {
                String prompt = "Hypothesis: " + discovery.getClaim() +
                        "\nNew observation: " + newObservation.getRawText();

                String result = contradictionClient.prompt()
                        .user(prompt)
                        .call()
                        .content()
                        .trim()
                        .toUpperCase();

                log.info("Contradiction check for case #{}: {}", discovery.getId(), result);

                if (result.startsWith("YES")) {
                    String updatedEvidence = discovery.getEvidenceAgainst() == null
                            || discovery.getEvidenceAgainst().isEmpty()
                            ? newObservation.getRawText()
                            : discovery.getEvidenceAgainst() + "; " + newObservation.getRawText();

                    discovery.setEvidenceAgainst(updatedEvidence);
                    discovery.setStatus("Investigating");
                    int newConfidence = Math.max(0, discovery.getConfidenceScore() - 20);
                    discovery.setConfidenceScore(newConfidence);
                    discoveryRepository.save(discovery);
                    log.info("Case #{} moved to Investigating", discovery.getId());
                }
            } catch (Exception e) {
                log.error("Contradiction check failed for case #{}: {}", discovery.getId(), e.getMessage());
            }
        }
    }
}