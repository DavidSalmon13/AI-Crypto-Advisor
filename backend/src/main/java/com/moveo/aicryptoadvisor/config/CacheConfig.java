package com.moveo.aicryptoadvisor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    /** Server-wide (not per-user) market-news cache, TTL 20 minutes — specs.md §4.3. */
    public static final String MARKET_NEWS_CACHE = "marketNews";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(MARKET_NEWS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(20))
                .maximumSize(16));
        return cacheManager;
    }
}
