package com.nowthink.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final EmailService emailService;

    public NotificationScheduler(EmailService emailService) {
        this.emailService = emailService;
    }

    // Runs every day at 8pm IST (14:30 UTC)
    @Scheduled(cron = "0 30 14 * * *")
    public void sendDailyReminder() {
        log.info("Running daily reminder scheduler");
        emailService.sendReminderEmail();
    }
}