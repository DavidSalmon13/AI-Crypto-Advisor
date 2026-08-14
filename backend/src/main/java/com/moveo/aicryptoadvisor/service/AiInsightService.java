package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.client.OpenRouterClient;
import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import com.moveo.aicryptoadvisor.entity.DailyContent;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI Insight of the Day — generated lazily on the first dashboard request per user per UTC
 * day and cached in {@code daily_content} (specs.md §7.2), never on a schedule.
 */
@Service
public class AiInsightService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightService.class);

    private static final String SYSTEM_PROMPT = """
            You are a crypto market assistant. Write one short, informative daily insight
            (3-5 sentences) for a retail crypto user. Be concrete and non-generic. Always end with
            the exact sentence: "This is not financial advice." Do not use markdown formatting.""";

    static final String FALLBACK_TEXT =
            "Markets move fast — check today's prices and headlines above to form your own view. "
                    + "This is not financial advice.";

    private static final String PAYLOAD_TEXT = "text";
    private static final String PAYLOAD_FALLBACK = "fallback";

    public record Insight(UUID id, String text, Instant generatedAt, boolean fallback) {
    }

    private final DailyContentCache dailyContentCache;
    private final OpenRouterClient openRouterClient;

    public AiInsightService(DailyContentCache dailyContentCache, OpenRouterClient openRouterClient) {
        this.dailyContentCache = dailyContentCache;
        this.openRouterClient = openRouterClient;
    }

    public Insight getOrGenerate(
            UUID userId,
            UserPreferences preferences,
            List<DashboardResponse.CoinPrice> coinPrices,
            LocalDate today
    ) {
        DailyContent row = dailyContentCache.getOrCreate(
                userId,
                DailyContentType.AI_INSIGHT,
                today,
                () -> generatePayload(preferences, coinPrices));
        return toInsight(row);
    }

    private Map<String, Object> generatePayload(
            UserPreferences preferences,
            List<DashboardResponse.CoinPrice> coinPrices
    ) {
        try {
            String text = openRouterClient.complete(SYSTEM_PROMPT, buildUserPrompt(preferences, coinPrices));
            return payload(text, false);
        } catch (RuntimeException e) {
            // Persisted with fallback:true so it stays identifiable later and is not silently
            // regenerated as if it were a real model response on the same day (specs.md §7.1).
            log.warn("OpenRouter insight generation failed — persisting fallback text", e);
            return payload(FALLBACK_TEXT, true);
        }
    }

    private static String buildUserPrompt(
            UserPreferences preferences,
            List<DashboardResponse.CoinPrice> coinPrices
    ) {
        String contentTypes = preferences.getContentTypes().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String interests = String.join(", ", preferences.getInterests());
        String changes = coinPrices.stream()
                .map(price -> price.id() + ": " + price.change24hPct() + "%")
                .collect(Collectors.joining(", "));

        return "Investor profile — type: " + preferences.getInvestorType().name()
                + ", prefers: " + contentTypes + ".\n"
                + "Coins they follow: " + interests + ".\n"
                + "Today's 24h price change for their coins: " + changes + ".\n"
                + "Write today's insight for this user.";
    }

    private static Map<String, Object> payload(String text, boolean fallback) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_TEXT, text);
        payload.put(PAYLOAD_FALLBACK, fallback);
        return payload;
    }

    private static Insight toInsight(DailyContent row) {
        Object text = row.getPayload().get(PAYLOAD_TEXT);
        Object fallback = row.getPayload().get(PAYLOAD_FALLBACK);
        return new Insight(
                row.getId(),
                text == null ? FALLBACK_TEXT : text.toString(),
                row.getCreatedAt(),
                Boolean.TRUE.equals(fallback));
    }
}
