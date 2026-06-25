package com.nowthink.service;

import com.nowthink.model.Observation;
import com.nowthink.repository.ObservationRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final ObservationRepository observationRepository;
    private final ChatClient subjectClient;

    @Value("${nowthink.notification.email:}")
    private String notificationEmail;

    public EmailService(JavaMailSender mailSender,
                        ObservationRepository observationRepository,
                        OpenAiChatModel model) {
        this.mailSender = mailSender;
        this.observationRepository = observationRepository;

        this.subjectClient = ChatClient.builder(model)
                .defaultSystem("""
                You are writing a personalized email subject line for Nowthink.
                Given a person's last observation, write ONE compelling subject line
                that makes them want to open the email and reflect more.
                
                Rules:
                - Maximum 8 words
                - Reference something specific from their observation
                - Make it feel personal, not like a notification
                - No emojis
                - No generic phrases like "Check in" or "Daily reminder"
                
                Return ONLY the subject line. Nothing else.
                """)
                .build();
    }

    public void sendReminderEmail() {
        if (notificationEmail == null || notificationEmail.isEmpty()) {
            log.warn("No notification email configured. Set nowthink.notification.email");
            return;
        }

        try {
            List<Observation> observations = observationRepository.findAllByOrderByCreatedAtDesc();

            String subject = "Nowthink — something to reflect on";
            String lastObservation = "";

            if (!observations.isEmpty()) {
                lastObservation = observations.get(0).getRawText();
                try {
                    subject = subjectClient.prompt()
                            .user(lastObservation)
                            .call()
                            .content()
                            .trim();
                } catch (Exception e) {
                    log.error("Subject generation failed: {}", e.getMessage());
                }
            }

            String body = buildEmailBody(observations, lastObservation);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notificationEmail);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("nowthink@noreply.com");

            mailSender.send(message);
            log.info("Reminder email sent to {}", notificationEmail);

        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage());
        }
    }

    private String buildEmailBody(List<Observation> observations, String lastObservation) {
        StringBuilder body = new StringBuilder();
        body.append("Nowthink noticed something.\n\n");

        if (!lastObservation.isEmpty()) {
            body.append("Your last observation:\n");
            body.append("\"").append(lastObservation).append("\"\n\n");
        }

        body.append("You have ").append(observations.size()).append(" observations filed.\n");
        body.append("The system is still investigating.\n\n");
        body.append("Come back and add what you noticed today.\n\n");
        body.append("—\n");
        body.append("nowthink-frontend.vercel.app\n");

        return body.toString();
    }
}