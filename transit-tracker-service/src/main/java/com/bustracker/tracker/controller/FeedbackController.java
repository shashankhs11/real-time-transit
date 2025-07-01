package com.bustracker.tracker.controller;

import com.bustracker.tracker.dto.FeedbackDto;
import com.bustracker.tracker.dto.FeedbackResponseDto;
import com.bustracker.tracker.service.FeedbackEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    private final FeedbackEmailService feedbackEmailService;

    public FeedbackController(FeedbackEmailService feedbackEmailService) {
        this.feedbackEmailService = feedbackEmailService;
    }

    @PostMapping
    public ResponseEntity<FeedbackResponseDto> submitFeedback(
            @Valid @RequestBody FeedbackDto feedbackDto,
            HttpServletRequest request) {
        
        logger.info("Received feedback submission from IP: {}", getClientIpAddress(request));
        
        try {
            String userAgent = request.getHeader("User-Agent");
            String remoteAddr = getClientIpAddress(request);
            
            feedbackEmailService.sendFeedbackEmail(
                feedbackDto.getFeedback(), 
                userAgent, 
                remoteAddr
            );
            
            logger.info("Feedback email sent successfully");
            
            return ResponseEntity.ok(new FeedbackResponseDto(
                true, 
                "Thank you for your feedback and for using TransSync! We really appreciate your input."
            ));
            
        } catch (Exception e) {
            logger.error("Failed to process feedback submission", e);
            
            return ResponseEntity.internalServerError().body(new FeedbackResponseDto(
                false, 
                "Sorry, we couldn't process your feedback at this time. Please try again later."
            ));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}