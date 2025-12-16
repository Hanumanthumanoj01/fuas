package com.frauas.servicemanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private String getEmail(String username) {
        // Mocks for testing. User must replace these emails.
        return switch (username) {
            case "pm_user" -> "hanumanthumanoj143@gmail.com";
            case "po_user" -> "hanumanthumanoj143@gmail.com";
            case "rp_user" -> "hanumanthumanoj143@gmail.com";
            default -> "hanumanthumanoj143@gmail.com";
        };
    }

    public void sendNotification(String toUsername, String subject, String body) {
        String toEmail = getEmail(toUsername);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Use mail sender username from application.properties
            message.setFrom("hanumanthumanoj143@gmail.com");
            message.setTo(toEmail);
            message.setSubject("FraUAS SMT: " + subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println(">>> EMAIL SENT to " + toUsername + ": " + subject);
        } catch (Exception e) {
            System.err.println("!!! EMAIL FAILED for " + toUsername + ". Check application.properties or App Password.");
            e.printStackTrace();
        }
    }
}