package com.moveo.aicryptoadvisor.controller;

import com.moveo.aicryptoadvisor.dto.request.FeedbackRequest;
import com.moveo.aicryptoadvisor.dto.response.FeedbackResponse;
import com.moveo.aicryptoadvisor.entity.ItemType;
import com.moveo.aicryptoadvisor.entity.User;
import com.moveo.aicryptoadvisor.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public FeedbackResponse vote(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FeedbackRequest request
    ) {
        return feedbackService.upsert(user.getId(), request);
    }

    @DeleteMapping("/{itemType}/{itemRef}")
    public ResponseEntity<Void> retract(
            @AuthenticationPrincipal User user,
            @PathVariable ItemType itemType,
            @PathVariable String itemRef
    ) {
        feedbackService.retract(user.getId(), itemType, itemRef);
        return ResponseEntity.noContent().build();
    }
}
