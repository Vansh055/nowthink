package com.nowthink.controller;

import com.nowthink.model.CheckIn;
import com.nowthink.repository.CheckInRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
@CrossOrigin(origins = "*")
public class CheckInController {

    private final ChatClient chatClient;
    private final CheckInRepository checkInRepository;

    public CheckInController(ChatClient.Builder builder, CheckInRepository checkInRepository) {
        this.checkInRepository = checkInRepository;
        this.chatClient = builder
                .defaultSystem("""
                You are Nowthink — a quiet, thoughtful companion that helps people 
                see patterns in themselves they never knew existed.
                
                When someone shares how their day went, ask ONE gentle follow-up question 
                that helps them notice something they might have missed about themselves.
                
                Never give advice. Never judge. Just reflect and ask.
                Keep responses short — 2 to 3 sentences maximum.
                """)
                .build();
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
        checkInRepository.save(checkIn);

        return response;
    }

    @GetMapping("/history")
    public java.util.List<CheckIn> getHistory() {
        return checkInRepository.findAllByOrderByCreatedAtDesc();
    }
}