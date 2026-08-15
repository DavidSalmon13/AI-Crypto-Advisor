package com.moveo.aicryptoadvisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.aicryptoadvisor.client.Meme;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The curated static meme pool (specs.md §5.2), loaded once at startup from data/memes.json.
 * Serves as {@code MemeService}'s fallback when the live Reddit meme feed is unavailable.
 */
@Component
public class MemePool {

    private final List<Meme> memes;

    public MemePool(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("data/memes.json").getInputStream()) {
            this.memes = List.copyOf(objectMapper.readValue(in, new TypeReference<List<Meme>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load data/memes.json", e);
        }
        if (memes.isEmpty()) {
            throw new IllegalStateException("data/memes.json is empty — the fallback meme pick needs at least one entry");
        }
    }

    public List<Meme> getMemes() {
        return memes;
    }
}
