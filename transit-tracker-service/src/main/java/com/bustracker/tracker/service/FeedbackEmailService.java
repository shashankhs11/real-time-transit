package com.bustracker.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FeedbackEmailService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackEmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender emailSender;

    @Value("${feedback.email.to}")
    private String feedbackEmailTo;

    @Value("${feedback.email.from}")
    private String feedbackEmailFrom;

    @Value("${feedback.email.subject:New Feedback - TransSync App}")
    private String emailSubject;

    public FeedbackEmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendFeedbackEmail(String feedback, String userAgent, String remoteAddr) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(feedbackEmailFrom);
            message.setTo(feedbackEmailTo);
            message.setSubject(emailSubject);
            
            String emailBody = buildEmailBody(feedback, userAgent, remoteAddr);
            message.setText(emailBody);
            
            emailSender.send(message);
            
            logger.info("Feedback email sent successfully to {}", feedbackEmailTo);
            
        } catch (Exception e) {
            logger.error("Failed to send feedback email", e);
            throw new RuntimeException("Failed to send feedback email", e);
        }
    }

    private String buildEmailBody(String feedback, String userAgent, String remoteAddr) {
        StringBuilder body = new StringBuilder();
        body.append("New feedback received for TransSync App\n\n");
        body.append("Timestamp: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n");
        body.append("IP Address: ").append(remoteAddr != null ? remoteAddr : "Unknown").append("\n");
        body.append("User Agent: ").append(userAgent != null ? userAgent : "Unknown").append("\n\n");
        body.append("Feedback:\n");
        body.append("----------\n");
        body.append(feedback).append("\n");
        body.append("----------\n\n");
        body.append("This is an automated message from TransSync feedback system.");
        
        return body.toString();
    }
}