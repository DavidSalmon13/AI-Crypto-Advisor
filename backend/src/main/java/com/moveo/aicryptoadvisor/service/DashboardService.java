package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.client.NewsArticle;
import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import com.moveo.aicryptoadvisor.entity.Feedback;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import com.moveo.aicryptoadvisor.exception.PreferencesNotSetException;
import com.moveo.aicryptoadvisor.repository.FeedbackRepository;
import com.moveo.aicryptoadvisor.repository.UserPreferencesRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Aggregates all four dashboard sections into the single {@code GET /api/dashboard} payload
 * (specs.md §4.3). Every section degrades to a fallback rather than failing the whole call.
 */
@Service
public class DashboardService {

    private final UserPreferencesRepository preferencesRepository;
    private final FeedbackRepository feedbackRepository;
    private final CoinPriceService coinPriceService;
    private final NewsService newsService;
    private final AiInsightService aiInsightService;
    private final MemeService memeService;

    public DashboardService(
            UserPreferencesRepository preferencesRepository,
            FeedbackRepository feedbackRepository,
            CoinPriceService coinPriceService,
            NewsService newsService,
            AiInsightService aiInsightService,
            MemeService memeService
    ) {
        this.preferencesRepository = preferencesRepository;
        this.feedbackRepository = feedbackRepository;
        this.coinPriceService = coinPriceService;
        this.newsService = newsService;
        this.aiInsightService = aiInsightService;
        this.memeService = memeService;
    }

    public DashboardResponse getDashboard(UUID userId) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(PreferencesNotSetException::new);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Fetched once and reused for the AI Insight prompt, so the insight can cite real
        // numbers without a second CoinGecko call (specs.md §7.1).
        List<DashboardResponse.CoinPrice> coinPrices = coinPriceService.getPrices(preferences.getInterests());
        List<NewsArticle> articles = newsService.getTopNews().articles();
        AiInsightService.Insight insight = aiInsightService.getOrGenerate(userId, preferences, coinPrices, today);
        MemeService.DailyMeme meme = memeService.getOrPick(userId, today);

        Map<String, Integer> votes = loadVotes(userId, articles, insight.id(), meme.id());

        return new DashboardResponse(
                today,
                coinPrices,
                articles.stream().map(article -> toNewsItem(article, votes)).toList(),
                new DashboardResponse.AiInsight(
                        insight.id(),
                        insight.text(),
                        insight.generatedAt(),
                        insight.fallback(),
                        votes.get(insight.id().toString())),
                new DashboardResponse.Meme(
                        meme.id(),
                        meme.imageUrl(),
                        meme.caption(),
                        votes.get(meme.id().toString())));
    }

    /**
     * One query for exactly the items on this dashboard. Refs are unambiguous across item
     * types — news ids carry a {@code cc-}/{@code fallback-} prefix, the others are UUIDs —
     * so a flat ref→vote map cannot collide.
     */
    private Map<String, Integer> loadVotes(
            UUID userId,
            List<NewsArticle> articles,
            UUID insightId,
            UUID memeId
    ) {
        List<String> refs = new ArrayList<>(articles.size() + 2);
        articles.forEach(article -> refs.add(article.id()));
        refs.add(insightId.toString());
        refs.add(memeId.toString());

        return feedbackRepository.findByUserIdAndItemRefIn(userId, refs).stream()
                .collect(Collectors.toMap(
                        Feedback::getItemRef,
                        feedback -> (int) feedback.getVote(),
                        (first, second) -> first,
                        HashMap::new));
    }

    private static DashboardResponse.NewsItem toNewsItem(NewsArticle article, Map<String, Integer> votes) {
        return new DashboardResponse.NewsItem(
                article.id(),
                article.title(),
                article.url(),
                article.source(),
                article.publishedAt(),
                votes.get(article.id()));
    }
}
