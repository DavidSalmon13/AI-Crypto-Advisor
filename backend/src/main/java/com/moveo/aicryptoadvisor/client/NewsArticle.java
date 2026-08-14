package com.moveo.aicryptoadvisor.client;

import java.time.Instant;

/**
 * Provider-agnostic news article, as returned by the news client and held in the
 * server-wide news cache. Deliberately carries no per-user state — the cache is shared
 * across all users, so {@code userVote} is joined on later, per request.
 */
public record NewsArticle(
        String id,
        String title,
        String url,
        String source,
        Instant publishedAt
) {
}
