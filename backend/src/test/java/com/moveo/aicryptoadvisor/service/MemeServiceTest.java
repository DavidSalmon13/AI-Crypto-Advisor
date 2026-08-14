package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.aicryptoadvisor.entity.DailyContent;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import com.moveo.aicryptoadvisor.repository.DailyContentRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the cache-hit vs cache-miss branches of the daily meme and the determinism of the
 * pick (specs.md §7.3) — a same-day retry must be idempotent even before the write lands.
 */
@ExtendWith(MockitoExtension.class)
class MemeServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 14);

    @Mock
    private DailyContentRepository dailyContentRepository;

    private final MemePool memePool = new MemePool(new ObjectMapper());

    private MemeService service() {
        return new MemeService(new DailyContentCache(dailyContentRepository), memePool);
    }

    @Test
    void cacheHit_returnsStoredMemeWithoutWriting() {
        DailyContent stored = new DailyContent(
                USER_ID, DailyContentType.MEME, TODAY,
                Map.of("memeId", "meme-007", "imageUrl", "https://example.test/7.png", "caption", "Stored caption"));
        when(dailyContentRepository.findByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.MEME, TODAY)).thenReturn(Optional.of(stored));

        MemeService.DailyMeme meme = service().getOrPick(USER_ID, TODAY);

        assertThat(meme.caption()).isEqualTo("Stored caption");
        assertThat(meme.imageUrl()).isEqualTo("https://example.test/7.png");
        verify(dailyContentRepository, never()).saveAndFlush(any());
    }

    @Test
    void cacheMiss_picksFromPoolAndPersists() {
        when(dailyContentRepository.findByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.MEME, TODAY)).thenReturn(Optional.empty());
        when(dailyContentRepository.saveAndFlush(any(DailyContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MemeService.DailyMeme meme = service().getOrPick(USER_ID, TODAY);

        MemePool.Meme expected = memePool.get(MemeService.pickIndex(USER_ID, TODAY, memePool.size()));
        assertThat(meme.caption()).isEqualTo(expected.caption());

        ArgumentCaptor<DailyContent> captor = ArgumentCaptor.forClass(DailyContent.class);
        verify(dailyContentRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPayload()).containsEntry("memeId", expected.id());
    }

    @Test
    void pickIsStableForSameUserAndDate() {
        int first = MemeService.pickIndex(USER_ID, TODAY, memePool.size());
        int second = MemeService.pickIndex(USER_ID, TODAY, memePool.size());

        assertThat(first).isEqualTo(second);
        assertThat(first).isBetween(0, memePool.size() - 1);
    }

    @Test
    void pickVariesAcrossDatesAndUsers() {
        int today = MemeService.pickIndex(USER_ID, TODAY, memePool.size());

        boolean anyDifferentDay = false;
        for (int dayOffset = 1; dayOffset <= 10; dayOffset++) {
            if (MemeService.pickIndex(USER_ID, TODAY.plusDays(dayOffset), memePool.size()) != today) {
                anyDifferentDay = true;
                break;
            }
        }
        assertThat(anyDifferentDay).as("meme should not be identical every day").isTrue();

        boolean anyDifferentUser = false;
        for (int i = 0; i < 10; i++) {
            if (MemeService.pickIndex(UUID.randomUUID(), TODAY, memePool.size()) != today) {
                anyDifferentUser = true;
                break;
            }
        }
        assertThat(anyDifferentUser).as("meme should not be identical for every user").isTrue();
    }
}
