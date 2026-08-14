package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moveo.aicryptoadvisor.dto.request.FeedbackRequest;
import com.moveo.aicryptoadvisor.dto.response.FeedbackResponse;
import com.moveo.aicryptoadvisor.entity.Feedback;
import com.moveo.aicryptoadvisor.entity.ItemType;
import com.moveo.aicryptoadvisor.repository.FeedbackRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Upsert-on-conflict and delete-when-absent — the two behaviours specs.md §4.4 is explicit
 * about, and the ones a naive insert/delete implementation would get wrong.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ITEM_REF = "cc-4821931";

    @Mock
    private FeedbackRepository feedbackRepository;

    private FeedbackService service() {
        return new FeedbackService(feedbackRepository);
    }

    private static FeedbackRequest request(int vote) {
        return new FeedbackRequest(ItemType.NEWS, ITEM_REF, vote);
    }

    private static Feedback existingVote(short vote) {
        return new Feedback(USER_ID, ItemType.NEWS, ITEM_REF, vote, LocalDate.of(2026, 8, 14));
    }

    @Test
    void firstVote_insertsNewRowWithServerSetDate() {
        when(feedbackRepository.findByUserIdAndItemTypeAndItemRef(USER_ID, ItemType.NEWS, ITEM_REF))
                .thenReturn(Optional.empty());
        when(feedbackRepository.saveAndFlush(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = service().upsert(USER_ID, request(1));

        assertThat(response.vote()).isEqualTo(1);
        assertThat(response.itemRef()).isEqualTo(ITEM_REF);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getItemDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
    }

    @Test
    void reVoting_flipsExistingRowInsteadOfInserting() {
        Feedback existing = existingVote((short) 1);
        when(feedbackRepository.findByUserIdAndItemTypeAndItemRef(USER_ID, ItemType.NEWS, ITEM_REF))
                .thenReturn(Optional.of(existing));
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = service().upsert(USER_ID, request(-1));

        assertThat(response.vote()).isEqualTo(-1);
        assertThat(existing.getVote()).isEqualTo((short) -1);
        verify(feedbackRepository).save(existing);
        verify(feedbackRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentFirstVotes_loserFlipsTheWinnersRow() {
        Feedback winner = existingVote((short) 1);
        when(feedbackRepository.findByUserIdAndItemTypeAndItemRef(USER_ID, ItemType.NEWS, ITEM_REF))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(feedbackRepository.saveAndFlush(any(Feedback.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = service().upsert(USER_ID, request(-1));

        assertThat(response.vote()).isEqualTo(-1);
        assertThat(winner.getVote()).isEqualTo((short) -1);
    }

    @Test
    void retract_existingVote_deletesIt() {
        Feedback existing = existingVote((short) 1);
        when(feedbackRepository.findByUserIdAndItemTypeAndItemRef(USER_ID, ItemType.NEWS, ITEM_REF))
                .thenReturn(Optional.of(existing));

        service().retract(USER_ID, ItemType.NEWS, ITEM_REF);

        verify(feedbackRepository).delete(existing);
    }

    @Test
    void retract_absentVote_isANoOpNotAnError() {
        when(feedbackRepository.findByUserIdAndItemTypeAndItemRef(USER_ID, ItemType.NEWS, ITEM_REF))
                .thenReturn(Optional.empty());

        service().retract(USER_ID, ItemType.NEWS, ITEM_REF);

        verify(feedbackRepository, never()).delete(any());
    }
}
