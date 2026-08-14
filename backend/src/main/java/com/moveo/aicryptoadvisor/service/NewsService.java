package com.moveo.aicryptoadvisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.aicryptoadvisor.client.NewsArticle;
import com.moveo.aicryptoadvisor.client.RssNewsClient;
import com.moveo.aicryptoadvisor.config.CacheConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Market news: one server-wide cache entry for "general crypto news", TTL 20 minutes
 * (specs.md §4.3). On any provider failure the static fallback file is served instead,
 * so a news outage never fails the dashboard call.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    /** Fixed page size, not paginated — specs.md §4.4. */
    private static final int NEWS_LIMIT = 10;

    /** Single shared key: the cache is server-wide, not per-user. */
    private static final String CACHE_KEY = "'latest'";

    /** Carries the fallback flag so a degraded result can be excluded from the cache. */
    public record NewsResult(List<NewsArticle> articles, boolean fallback) {
    }

    private final RssNewsClient rssNewsClient;
    private final List<NewsArticle> fallbackArticles;

    public NewsService(RssNewsClient rssNewsClient, ObjectMapper objectMapper) {
        this.rssNewsClient = rssNewsClient;
        try (InputStream in = new ClassPathResource("data/news-fallback.json").getInputStream()) {
            this.fallbackArticles = List.copyOf(objectMapper.readValue(in, new TypeReference<List<NewsArticle>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load data/news-fallback.json", e);
        }
    }

    /**
     * Fallback results are deliberately not cached ({@code unless}), so a transient provider
     * blip self-heals on the next request instead of pinning static headlines for 20 minutes.
     */
    @Cacheable(cacheNames = CacheConfig.MARKET_NEWS_CACHE, key = CACHE_KEY, unless = "#result.fallback()")
    public NewsResult getTopNews() {
        try {
            List<NewsArticle> articles = rssNewsClient.fetchLatestNews();
            if (articles.isEmpty()) {
                log.warn("RSS news feeds returned zero articles — serving static fallback news");
                return new NewsResult(fallbackArticles, true);
            }
            return new NewsResult(articles.stream().limit(NEWS_LIMIT).toList(), false);
        } catch (RuntimeException e) {
            log.warn("RSS news fetch failed — serving static fallback news", e);
            return new NewsResult(fallbackArticles, true);
        }
    }
}
