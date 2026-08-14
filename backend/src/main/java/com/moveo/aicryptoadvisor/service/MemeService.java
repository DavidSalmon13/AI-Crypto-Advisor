package com.moveo.aicryptoadvisor.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Service;

/**
 * Fun Crypto Meme — picked fresh from the static pool on every dashboard load, per the
 * assignment's "shown dynamically each time the dashboard updates" (unlike the AI Insight,
 * which is intentionally cached once per user per day). Voting targets the pool entry's own
 * stable id (e.g. {@code meme-014}) directly, since there's no per-request row to key off.
 */
@Service
public class MemeService {

    public record DailyMeme(String id, String imageUrl, String caption) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemePool memePool;

    public MemeService(MemePool memePool) {
        this.memePool = memePool;
    }

    public DailyMeme pickRandom() {
        MemePool.Meme meme = memePool.get(RANDOM.nextInt(memePool.size()));
        return new DailyMeme(meme.id(), meme.imageUrl(), meme.caption());
    }
}
