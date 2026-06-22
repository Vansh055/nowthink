package com.nowthink.controller;

import com.nowthink.model.CheckIn;
import com.nowthink.repository.CheckInRepository;
import com.nowthink.service.PatternService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/checkin")
@CrossOrigin(origins = "*")
public class CheckInController {

    private final ChatClient chatClient;
    private final ChatClient scoringClient;
    private final CheckInRepository checkInRepository;
    private final PatternService patternService;

    public CheckInController(OpenAiChatModel model,
                             CheckInRepository checkInRepository,
                             PatternService patternService) {
        this.checkInRepository = checkInRepository;
        this.patternService = patternService;

        this.chatClient = ChatClient.builder(model)
                .defaultSystem("""
                You are Nowthink — a quiet, thoughtful companion that helps people 
                see patterns in themselves they never knew existed.
                
                When someone shares how their day went, ask ONE gentle follow-up question 
                that helps them notice something they might have missed about themselves.
                
                Never give advice. Never judge. Just reflect and ask.
                Keep responses short — 2 to 3 sentences maximum.
                """)
                .build();

        this.scoringClient = ChatClient.builder(model)
                .defaultSystem("""
                You are an energy scorer. Given a person's message about their day,
                respond with ONLY a single integer from 1 to 10 representing their energy level.
                1 = completely drained, 10 = highly energized.
                No explanation. No punctuation. Just the number.
                """)
                .build();
    }

    private int scoreEnergy(String message) {
        try {
            String score = scoringClient.prompt()
                    .user(message)
                    .call()
                    .content()
                    .trim();
            return Integer.parseInt(score.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 5;
        }
    }

    @PostMapping
    public String checkIn(@RequestBody String message) {
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();

        CheckIn checkIn = new CheckIn();
        checkIn.setUserMessage(message);
        checkIn.setNowthinkResponse(response);
        checkIn.setEnergyScore(scoreEnergy(message));
        checkInRepository.save(checkIn);

        return response;
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> checkInStream(@RequestBody String message) {
        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    CheckIn checkIn = new CheckIn();
                    checkIn.setUserMessage(message);
                    checkIn.setNowthinkResponse(fullResponse.toString());
                    checkIn.setEnergyScore(scoreEnergy(message));
                    checkInRepository.save(checkIn);
                });
    }

    @GetMapping("/history")
    public List<CheckIn> getHistory() {
        return checkInRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/patterns")
    public String getPatterns() {
        return patternService.detectPatterns();
    }

    @GetMapping("/timeline")
    public List<CheckIn> getTimeline() {
        return checkInRepository.findAllByOrderByCreatedAtAsc();
    }
}