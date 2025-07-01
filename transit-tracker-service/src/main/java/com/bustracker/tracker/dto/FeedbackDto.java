package com.bustracker.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackDto {

    @NotNull(message = "Feedback is required")
    @NotBlank(message = "Feedback cannot be empty or contain only whitespace")
    @Size(min = 1, max = 2000, message = "Feedback must be between 1 and 2000 characters")
    private String feedback;

    public FeedbackDto() {}

    public FeedbackDto(String feedback) {
        this.feedback = feedback;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}