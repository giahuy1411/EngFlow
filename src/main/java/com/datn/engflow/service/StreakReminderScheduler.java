package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreakReminderScheduler {

    private final StreakService streakService;
    private final EmailService emailService;

    /**
     * Run every day at 20:00 (8:00 PM) to remind users who haven't studied yet today.
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendDailyStreakReminders() {
        log.info("Starting daily streak reminder job...");

        List<User> usersToRemind = streakService.getUsersWhoDidNotStudyToday();
        
        if (usersToRemind.isEmpty()) {
            log.info("No users need reminding today. Everyone studied! Awesome.");
            return;
        }

        log.info("Found {} users who haven't studied today. Sending emails...", usersToRemind.size());

        for (User user : usersToRemind) {
            try {
                emailService.sendStreakReminder(user.getEmail(), user.getFullName(), user.getCurrentStreak());
            } catch (Exception e) {
                log.error("Error sending streak reminder to user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Finished daily streak reminder job.");
    }
}
