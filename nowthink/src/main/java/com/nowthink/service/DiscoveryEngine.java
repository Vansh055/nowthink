package com.nowthink.service;

import com.nowthink.model.Discovery;
import com.nowthink.model.Observation;
import com.nowthink.repository.DiscoveryRepository;
import com.nowthink.repository.ObservationRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiscoveryEngine {

    private final ObservationRepository observationRepository;
    private final DiscoveryRepository discoveryRepository;
    private final ChatClient discoveryClient;
    private final ChatClient confidenceClient;

    public DiscoveryEngine(ObservationRepository observationRepository,
                           DiscoveryRepository discoveryRepository,
                           OpenAiChatModel model) {
        this.observationRepository = observationRepository;
        this.discoveryRepository = discoveryRepository;

        this.discoveryClient = ChatClient.builder(model)
                .defaultSystem("""
                You are a pattern detective analyzing a person's observations.
                Given observations, generate ONE discovery about this person.
                Respond ONLY with this exact format, no extra text, no markdown:
                CLAIM: [one sentence hypothesis about the person]
                TYPE: [exactly one of: Pattern, Contradiction, BlindSpot, Evolution]
                EVIDENCE_FOR: [quote directly from observations that support the claim]
                EVIDENCE_AGAINST: [quote directly from observations that contradict, or write NONE]
                """)
                .build();

        this.confidenceClient = ChatClient.builder(model)
                .defaultSystem("""
                You are a confidence scorer.
                Given a hypothesis and evidence, return ONLY a number from 0 to 100.
                No words. No explanation. Just the number.
                """)
                .build();
    }

    public Discovery generateDiscovery(String userId) {
        List<Observation> observations = observationRepository
                .findByUserIdOrderByCreatedAtAsc(userId);

        if (observations.size() < 3) {
            Discovery d = new Discovery();
            d.setUserId(userId);
            d.setClaim("Add " + (3 - observations.size()) + " more observations to unlock your first discovery.");
            d.setStatus("pending");
            d.setConfidenceScore(0);
            d.setDiscoveryType("none");
            d.setEvidenceFor("");
            d.setEvidenceAgainst("");
            return d;
        }

        String observationText = observations.stream()
                .map(o -> "- " + o.getRawText())
                .collect(Collectors.joining("\n"));

        try {
            String response = discoveryClient.prompt()
                    .user("Observations:\n" + observationText)
                    .call()
                    .content();

            String claim = extractField(response, "CLAIM:");
            String type = extractField(response, "TYPE:");
            String evidenceFor = extractField(response, "EVIDENCE_FOR:");
            String evidenceAgainst = extractField(response, "EVIDENCE_AGAINST:");

            if (claim.isEmpty()) claim = "A pattern was detected but could not be parsed clearly.";
            if (type.isEmpty()) type = "Pattern";
            if (evidenceAgainst.equalsIgnoreCase("NONE")) evidenceAgainst = "";

            int confidence = 60;
            try {
                String confRaw = confidenceClient.prompt()
                        .user("Hypothesis: " + claim + "\nFor: " + evidenceFor + "\nAgainst: " + evidenceAgainst)
                        .call()
                        .content()
                        .trim();
                confidence = Integer.parseInt(confRaw.replaceAll("[^0-9]", ""));
                confidence = Math.min(100, Math.max(0, confidence));
            } catch (Exception ignored) {}

            String status = confidence >= 60 ? "Supported" : confidence >= 40 ? "Investigating" : "Refuted";

            Discovery discovery = new Discovery();
            discovery.setUserId(userId);
            discovery.setClaim(claim);
            discovery.setDiscoveryType(type);
            discovery.setEvidenceFor(evidenceFor);
            discovery.setEvidenceAgainst(evidenceAgainst);
            discovery.setConfidenceScore(confidence);
            discovery.setStatus(status);
            discoveryRepository.save(discovery);

            return discovery;

        } catch (Exception e) {
            Discovery d = new Discovery();
            d.setUserId(userId);
            d.setClaim("Discovery generation failed: " + e.getMessage());
            d.setStatus("error");
            d.setConfidenceScore(0);
            d.setDiscoveryType("none");
            d.setEvidenceFor("");
            d.setEvidenceAgainst("");
            return d;
        }
    }

    public List<Discovery> getAllDiscoveries(String userId) {
        return discoveryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String extractField(String response, String field) {
        try {
            int start = response.indexOf(field);
            if (start == -1) return "";
            int valueStart = start + field.length();
            int nextLine = response.indexOf("\n", valueStart);
            if (nextLine == -1) nextLine = response.length();
            return response.substring(valueStart, nextLine).trim();
        } catch (Exception e) {
            return "";
        }
    }
}