package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.dto.request.FeedbackRequest;
import com.moveo.aicryptoadvisor.dto.response.FeedbackResponse;
import com.moveo.aicryptoadvisor.entity.Feedback;
import com.moveo.aicryptoadvisor.entity.ItemType;
import com.moveo.aicryptoadvisor.repository.FeedbackRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Vote persistence (specs.md §4.4). Upsert is keyed on the {@code (user_id, item_type,
 * item_ref)} unique constraint, so re-voting flips the existing row instead of accumulating
 * duplicates — which is what keeps the §7.4 training data one-label-per-user-per-item.
 *
 * <p>Deliberately not {@code @Transactional}, for the same reason as {@link DailyContentCache}:
 * a double-click can race two inserts, and the loser needs a working follow-up read that a
 * rollback-only transaction would deny it.
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public FeedbackResponse upsert(UUID userId, FeedbackRequest request) {
        short vote = request.vote().shortValue();

        return feedbackRepository
                .findByUserIdAndItemTypeAndItemRef(userId, request.itemType(), request.itemRef())
                .map(existing -> flip(existing, vote))
                .orElseGet(() -> insertOrFlip(userId, request, vote));
    }

    private FeedbackResponse flip(Feedback existing, short vote) {
        existing.changeVote(vote);
        return FeedbackResponse.from(feedbackRepository.save(existing));
    }

    private FeedbackResponse insertOrFlip(UUID userId, FeedbackRequest request, short vote) {
        Feedback candidate = new Feedback(
                userId,
                request.itemType(),
                request.itemRef(),
                vote,
                // Server-set, never client-supplied (specs.md §4.4).
                LocalDate.now(ZoneOffset.UTC));
        try {
            return FeedbackResponse.from(feedbackRepository.saveAndFlush(candidate));
        } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent vote insert for user {} on {} — flipping the existing row", userId, request.itemRef());
            return feedbackRepository
                    .findByUserIdAndItemTypeAndItemRef(userId, request.itemType(), request.itemRef())
                    .map(existing -> flip(existing, vote))
                    .orElseThrow(() -> e);
        }
    }

    /** Idempotent: retracting a vote that was never cast is a no-op, not a 404 (specs.md §4.4). */
    public void retract(UUID userId, ItemType itemType, String itemRef) {
        feedbackRepository
                .findByUserIdAndItemTypeAndItemRef(userId, itemType, itemRef)
                .ifPresent(feedbackRepository::delete);
    }
}
