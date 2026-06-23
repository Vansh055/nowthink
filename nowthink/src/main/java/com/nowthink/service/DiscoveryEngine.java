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
    private final ChatClient hypothesisClient;
    private final ChatClient evidenceClient;
    private final ChatClient confidenceClient;

    public DiscoveryEngine(ObservationRepository observationRepository,
                           DiscoveryRepository discoveryRepository,
                           OpenAiChatModel model) {
        this.observationRepository = observationRepository;
        this.discoveryRepository = discoveryRepository;

        this.hypothesisClient = ChatClient.builder(model)
                .defaultSystem("""
                You are a pattern detective analyzing a person's observations over time.
                
                Given a list of observations, generate ONE hypothesis about this person
                that they probably don't know about themselves.
                
                Rules:
                - Be specific, not generic
                - Reference actual patterns from the observations
                - Format your response as JSON exactly like this:
                {
                  "claim": "one sentence hypothesis",
                  "discoveryType": "Pattern|Contradiction|BlindSpot|Evolution",
                  "evidenceFor": "observation ids that support this, comma separated",
                  "evidenceAgainst": "observation ids that contradict this, comma separated"
                }
                - Only return JSON. No explanation. No markdown.
                """)
                .build();

        this.evidenceClient = ChatClient.builder(model)
                .defaultSystem("""
                You are an evidence analyst.
                Given a hypothesis and a list of observations with their IDs,
                find which observations support the hypothesis and which contradict it.
                
                Return ONLY JSON:
                {
                  "evidenceFor": "quoted text from supporting observations",
                  "evidenceAgainst": "quoted text from contradicting observations"
                }
                """)
                .build();

        this.confidenceClient = ChatClient.builder(model)
                .defaultSystem("""
                You are a confidence scorer.
                Given a hypothesis, supporting evidence, and contradicting evidence,
                return ONLY a single integer from 0 to 100 representing confidence.
                No explanation. Just the number.
                """)
                .build();
    }

    public Discovery generateDiscovery() {
        List<Observation> observations = observationRepository.findAllByOrderByCreatedAtAsc();

        if (observations.size() < 3) {
            Discovery d = new Discovery();
            d.setClaim("Not enough observations yet. Add " + (3 - observations.size()) + " more to unlock your first discovery.");
            d.setStatus("pending");
            d.setConfidenceScore(0);
            d.setDiscoveryType("none");
            d.setEvidenceFor("");
            d.setEvidenceAgainst("");
            return d;
        }

        String observationText = observations.stream()
                .map(o -> "ID " + o.getId() + ": " + o.getRawText())
                .collect(Collectors.joining("\n"));

        String hypothesisJson = hypothesisClient.prompt()
                .user("Here are the observations:\n\n" + observationText)
                .call()
                .content();

        String claim = extractJson(hypothesisJson, "claim");
        String discoveryType = extractJson(hypothesisJson, "discoveryType");

        String evidenceJson = evidenceClient.prompt()
                .user("Hypothesis: " + claim + "\n\nObservations:\n" + observationText)
                .call()
                .content();

        String evidenceFor = extractJson(evidenceJson, "evidenceFor");
        String evidenceAgainst = extractJson(evidenceJson, "evidenceAgainst");

        String confidenceRaw = confidenceClient.prompt()
                .user("Hypothesis: " + claim + "\nSupporting: " + evidenceFor + "\nContradicting: " + evidenceAgainst)
                .call()
                .content()
                .trim();

        int confidence = 50;
        try {
            confidence = Integer.parseInt(confidenceRaw.replaceAll("[^0-9]", ""));
            confidence = Math.min(100, Math.max(0, confidence));
        } catch (Exception ignored) {}

        String status = confidence >= 60 ? "Supported" : confidence >= 40 ? "Investigating" : "Refuted";

        Discovery discovery = new Discovery();
        discovery.setClaim(claim);
        discovery.setDiscoveryType(discoveryType);
        discovery.setEvidenceFor(evidenceFor);
        discovery.setEvidenceAgainst(evidenceAgainst);
        discovery.setConfidenceScore(confidence);
        discovery.setStatus(status);
        discoveryRepository.save(discovery);

        return discovery;
    }

    public List<Discovery> getAllDiscoveries() {
        return discoveryRepository.findAllByOrderByCreatedAtDesc();
    }

    private String extractJson(String json, String key) {
        try {
            String clean = json.replaceAll("```json", "").replaceAll("```", "").trim();
            int keyIndex = clean.indexOf("\"" + key + "\"");
            if (keyIndex == -1) return "";
            int colonIndex = clean.indexOf(":", keyIndex);
            int quoteStart = clean.indexOf("\"", colonIndex + 1);
            int quoteEnd = clean.indexOf("\"", quoteStart + 1);
            return clean.substring(quoteStart + 1, quoteEnd);
        } catch (Exception e) {
            return "";
        }
    }
}