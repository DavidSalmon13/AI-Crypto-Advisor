package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The meme is picked fresh on every dashboard load (not cached per day, unlike the AI
 * Insight) — see specs.md §7.3 and the assignment's "shown dynamically each time the
 * dashboard updates."
 */
class MemeServiceTest {

    private final MemePool memePool = new MemePool(new ObjectMapper());
    private final MemeService service = new MemeService(memePool);

    @Test
    void pickRandom_returnsAKnownPoolEntry() {
        MemeService.DailyMeme meme = service.pickRandom();

        assertThat(memePool.getMemes())
                .anySatisfy(pooled -> {
                    assertThat(meme.id()).isEqualTo(pooled.id());
                    assertThat(meme.imageUrl()).isEqualTo(pooled.imageUrl());
                    assertThat(meme.caption()).isEqualTo(pooled.caption());
                });
    }

    @Test
    void pickRandom_variesAcrossCalls() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            seen.add(service.pickRandom().id());
        }

        // With a 25-entry pool and 50 draws, seeing only one distinct id would be an
        // astronomically unlikely coincidence rather than a real "stable pick" behavior.
        assertThat(seen).as("repeated picks should not all be identical").hasSizeGreaterThan(1);
    }
}
