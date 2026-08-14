package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.entity.DailyContent;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fun Crypto Meme of the Day — picked deterministically from the static pool and cached in
 * {@code daily_content} exactly like the AI Insight (specs.md §7.3).
 */
@Service
public class MemeService {

    private static final String PAYLOAD_MEME_ID = "memeId";
    private static final String PAYLOAD_IMAGE_URL = "imageUrl";
    private static final String PAYLOAD_CAPTION = "caption";

    public record DailyMeme(UUID id, String imageUrl, String caption) {
    }

    private final DailyContentCache dailyContentCache;
    private final MemePool memePool;

    public MemeService(DailyContentCache dailyContentCache, MemePool memePool) {
        this.dailyContentCache = dailyContentCache;
        this.memePool = memePool;
    }

    public DailyMeme getOrPick(UUID userId, LocalDate today) {
        DailyContent row = dailyContentCache.getOrCreate(
                userId,
                DailyContentType.MEME,
                today,
                () -> pickPayload(userId, today));
        return toDailyMeme(row);
    }

    private Map<String, Object> pickPayload(UUID userId, LocalDate today) {
        MemePool.Meme meme = memePool.get(pickIndex(userId, today, memePool.size()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_MEME_ID, meme.id());
        payload.put(PAYLOAD_IMAGE_URL, meme.imageUrl());
        payload.put(PAYLOAD_CAPTION, meme.caption());
        return payload;
    }

    /**
     * Deterministic rather than random, so a retry within the same day is idempotent even
     * before the cache write lands (specs.md §4.3).
     */
    static int pickIndex(UUID userId, LocalDate date, int poolSize) {
        return Math.floorMod((userId.toString() + date).hashCode(), poolSize);
    }

    private static DailyMeme toDailyMeme(DailyContent row) {
        Object imageUrl = row.getPayload().get(PAYLOAD_IMAGE_URL);
        Object caption = row.getPayload().get(PAYLOAD_CAPTION);
        return new DailyMeme(
                row.getId(),
                imageUrl == null ? null : imageUrl.toString(),
                caption == null ? null : caption.toString());
    }
}
