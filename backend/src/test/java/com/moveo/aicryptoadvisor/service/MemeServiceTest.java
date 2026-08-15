package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.aicryptoadvisor.client.Meme;
import com.moveo.aicryptoadvisor.client.RedditMemeClient;
import com.moveo.aicryptoadvisor.config.CacheConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Covers the live Reddit pool's cache-hit vs cache-miss branches, and the fallback to the
 * static curated pool when the feed is unavailable — specs.md §7.3.
 */
@ExtendWith(MockitoExtension.class)
class MemeServiceTest {

    private final MemePool memePool = new MemePool(new ObjectMapper());

    @Mock
    private RedditMemeClient redditMemeClient;

    private MemeService service() {
        return new MemeService(redditMemeClient, memePool, new ConcurrentMapCacheManager(CacheConfig.REDDIT_MEMES_CACHE));
    }

    private static List<Meme> redditMemes() {
        return List.of(
                new Meme("reddit-abc123", "https://i.redd.it/abc123.jpg", "Number go up"),
                new Meme("reddit-def456", "https://i.redd.it/def456.png", "Number go down"));
    }

    @Test
    void feedAvailable_picksFromLiveRedditPool() {
        when(redditMemeClient.fetchTopMemes()).thenReturn(redditMemes());

        MemeService.DailyMeme meme = service().pickRandom();

        assertThat(meme.id()).startsWith("reddit-");
    }

    @Test
    void feedAvailable_secondCallHitsCacheInsteadOfTheFeedAgain() {
        when(redditMemeClient.fetchTopMemes()).thenReturn(redditMemes());
        MemeService service = service();

        service.pickRandom();
        service.pickRandom();

        verify(redditMemeClient, times(1)).fetchTopMemes();
    }

    @Test
    void feedReturnsNothingUsable_fallsBackToStaticPool() {
        when(redditMemeClient.fetchTopMemes()).thenReturn(List.of());

        MemeService.DailyMeme meme = service().pickRandom();

        assertThat(memePool.getMemes())
                .anySatisfy(pooled -> assertThat(meme.id()).isEqualTo(pooled.id()));
    }

    @Test
    void feedReturnsNothingUsable_fallbackIsNotCached_retriesFeedOnNextCall() {
        when(redditMemeClient.fetchTopMemes()).thenReturn(List.of());
        MemeService service = service();

        service.pickRandom();
        service.pickRandom();

        verify(redditMemeClient, times(2)).fetchTopMemes();
    }

    @Test
    void fallbackPool_variesAcrossCalls() {
        when(redditMemeClient.fetchTopMemes()).thenReturn(List.of());
        MemeService service = service();

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            seen.add(service.pickRandom().id());
        }

        // With a 25-entry pool and 50 draws, seeing only one distinct id would be an
        // astronomically unlikely coincidence rather than a real "stable pick" behavior.
        assertThat(seen).as("repeated picks should not all be identical").hasSizeGreaterThan(1);
    }
}
