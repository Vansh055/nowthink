package com.nowthink.service;

import com.nowthink.model.CheckIn;
import com.nowthink.repository.CheckInRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatternService {

    private final CheckInRepository checkInRepository;
    private final ChatClient chatClient;

    public PatternService(CheckInRepository checkInRepository, ChatClient.Builder builder) {
        this.checkInRepository = checkInRepository;
        this.chatClient = builder
                .defaultSystem("""
                You are Nowthink — a deep pattern recognizer.
                
                You will be given a person's daily check-ins over multiple days.
                Your job is to find ONE pattern they haven't noticed about themselves.
                
                It could be about their energy, their mood triggers, what drains them,
                what excites them, when they feel stuck, or how they talk about themselves.
                
                Be specific. Be honest. Be gentle.
                Don't give advice. Just reflect the pattern back to them.
                
                Format: 2-3 sentences maximum. Start with "I've noticed..."
                """)
                .build();
    }

    public String detectPatterns() {
        List<CheckIn> checkIns = checkInRepository.findAllByOrderByCreatedAtDesc();

        if (checkIns.size() < 3) {
            return "Check in for a few more days — Nowthink needs at least 3 entries to start seeing patterns.";
        }

        String allCheckIns = checkIns.stream()
                .map(c -> "Day " + c.getCreatedAt().toLocalDate() + ": " + c.getUserMessage())
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .user("Here are my recent check-ins:\n\n" + allCheckIns + "\n\nWhat pattern do you notice?")
                .call()
                .content();
    }
}