package com.moveo.aicryptoadvisor.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The single aggregated dashboard payload (specs.md §4.3). {@code userVote} is resolved
 * server-side from the caller's feedback rows, so the client never reconciles vote state.
 */
public record DashboardResponse(
        LocalDate date,
        List<CoinPrice> coinPrices,
        List<NewsItem> marketNews,
        AiInsight aiInsight,
        Meme meme
) {

    public record CoinPrice(
            String id,
            String symbol,
            String name,
            BigDecimal priceUsd,
            BigDecimal change24hPct
    ) {
    }

    public record NewsItem(
            String id,
            String title,
            String url,
            String source,
            Instant publishedAt,
            Integer userVote
    ) {
    }

    public record AiInsight(
            UUID id,
            String text,
            Instant generatedAt,
            boolean fallback,
            Integer userVote
    ) {
    }

    public record Meme(
            UUID id,
            String imageUrl,
            String caption,
            Integer userVote
    ) {
    }
}
