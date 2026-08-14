package com.moveo.aicryptoadvisor.dto.request;

import com.moveo.aicryptoadvisor.entity.ItemType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A 👍/👎 vote (specs.md §4.4). {@code item_date} is deliberately absent — the server sets
 * it from the current UTC day rather than trusting the client.
 */
public record FeedbackRequest(
        @NotNull ItemType itemType,
        @NotBlank @Size(max = 255) String itemRef,
        @NotNull Integer vote
) {

    /** {@code vote} is a two-valued choice, not a range — 0 is not a "neutral" vote. */
    @AssertTrue(message = "must be 1 (thumbs up) or -1 (thumbs down)")
    public boolean isVoteValid() {
        return vote == null || vote == 1 || vote == -1;
    }
}
