package com.bustracker.tracker.controller;

import com.bustracker.tracker.dto.FeedbackResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FeedbackResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = errors.values().iterator().next(); // Get first validation error
        
        logger.warn("Validation failed for feedback submission: {}", errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new FeedbackResponseDto(
            false, 
            errorMessage
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FeedbackResponseDto> handleGenericException(Exception ex) {
        logger.error("Unexpected error in feedback submission", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new FeedbackResponseDto(
            false, 
            "Sorry, we couldn't process your feedback at this time. Please try again later."
        ));
    }
}