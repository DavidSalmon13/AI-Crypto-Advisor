package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.client.Meme;
import com.moveo.aicryptoadvisor.client.RedditMemeClient;
import com.moveo.aicryptoadvisor.config.CacheConfig;
import java.security.SecureRandom;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Fun Crypto Meme — picked fresh from a live pool on every dashboard load (unlike the AI
 * Insight, which is cached once per user per day — specs.md §7.3). The live pool (top image
 * posts from r/CryptoCurrencyMemes) is cached server-wide for 20 minutes, same policy as
 * Market News, so a dashboard load doesn't mean a live Reddit call every time. On any fetch
 * failure — or if Reddit returns nothing usable — the static curated pool (data/memes.json)
 * is served instead, and that miss is never cached, so the feed is retried on the next call
 * rather than pinning the fallback for 20 minutes.
 *
 * <p>Cache access is manual (via {@link CacheManager}) rather than {@code @Cacheable}: the
 * cached lookup happens from {@link #pickRandom()} in this same class, and self-invocation
 * bypasses Spring's proxy-based caching, so the annotation would silently do nothing here.
 */
@Service
public class MemeService {

    private static final Logger log = LoggerFactory.getLogger(MemeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CACHE_KEY = "latest";

    public record DailyMeme(String id, String imageUrl, String caption) {
    }

    private final RedditMemeClient redditMemeClient;
    private final MemePool memePool;
    private final Cache redditMemeCache;

    public MemeService(RedditMemeClient redditMemeClient, MemePool memePool, CacheManager cacheManager) {
        this.redditMemeClient = redditMemeClient;
        this.memePool = memePool;
        this.redditMemeCache = cacheManager.getCache(CacheConfig.REDDIT_MEMES_CACHE);
    }

    public DailyMeme pickRandom() {
        List<Meme> pool = getPool();
        Meme meme = pool.get(RANDOM.nextInt(pool.size()));
        return new DailyMeme(meme.id(), meme.imageUrl(), meme.caption());
    }

    private List<Meme> getPool() {
        List<Meme> cached = redditMemeCache.get(CACHE_KEY, List.class);
        if (cached != null) {
            return cached;
        }
        List<Meme> live = redditMemeClient.fetchTopMemes();
        if (!live.isEmpty()) {
            redditMemeCache.put(CACHE_KEY, live);
            return live;
        }
        log.warn("Reddit meme feed returned no usable posts — serving the static meme pool");
        return memePool.getMemes();
    }
}
