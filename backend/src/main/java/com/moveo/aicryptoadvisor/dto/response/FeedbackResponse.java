package com.moveo.aicryptoadvisor.dto.response;

import com.moveo.aicryptoadvisor.entity.Feedback;
import com.moveo.aicryptoadvisor.entity.ItemType;
import java.util.UUID;

public record FeedbackResponse(UUID id, ItemType itemType, String itemRef, int vote) {

    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getItemType(),
                feedback.getItemRef(),
                feedback.getVote());
    }
}
