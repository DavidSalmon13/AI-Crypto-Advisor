package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.moveo.aicryptoadvisor.client.OpenRouterClient;
import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.DailyContent;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import com.moveo.aicryptoadvisor.repository.DailyContentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the cache-hit vs cache-miss branches of the daily insight, plus the
 * OpenRouter-failure fallback — the "risky logic" specs.md's testing scope calls out.
 */
@ExtendWith(MockitoExtension.class)
class AiInsightServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 14);

    @Mock
    private DailyContentRepository dailyContentRepository;

    @Mock
    private OpenRouterClient openRouterClient;

    private AiInsightService service() {
        return new AiInsightService(new DailyContentCache(dailyContentRepository), openRouterClient);
    }

    private static UserPreferences preferences() {
        return new UserPreferences(
                USER_ID,
                InvestorType.HODLER,
                List.of("bitcoin", "ethereum"),
                List.of(ContentType.MARKET_NEWS));
    }

    private static List<DashboardResponse.CoinPrice> coinPrices() {
        return List.of(new DashboardResponse.CoinPrice(
                "bitcoin", "BTC", "Bitcoin", new BigDecimal("61234.12"), new BigDecimal("2.34")));
    }

    @Test
    void cacheHit_returnsStoredInsightWithoutCallingModel() {
        DailyContent stored = new DailyContent(
                USER_ID, DailyContentType.AI_INSIGHT, TODAY,
                Map.of("text", "Yesterday's stored insight.", "fallback", false));
        when(dailyContentRepository.findByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.AI_INSIGHT, TODAY)).thenReturn(Optional.of(stored));

        AiInsightService.Insight insight = service().getOrGenerate(USER_ID, preferences(), coinPrices(), TODAY);

        assertThat(insight.text()).isEqualTo("Yesterday's stored insight.");
        assertThat(insight.fallback()).isFalse();
        verifyNoInteractions(openRouterClient);
        verify(dailyContentRepository, never()).saveAndFlush(any());
    }

    @Test
    void cacheMiss_generatesPersistsAndReturnsModelText() {
        when(dailyContentRepository.findByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.AI_INSIGHT, TODAY)).thenReturn(Optional.empty());
        when(dailyContentRepository.saveAndFlush(any(DailyContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(openRouterClient.complete(anyString(), anyString())).thenReturn("Fresh model insight.");

        AiInsightService.Insight insight = service().getOrGenerate(USER_ID, preferences(), coinPrices(), TODAY);

        assertThat(insight.text()).isEqualTo("Fresh model insight.");
        assertThat(insight.fallback()).isFalse();

        ArgumentCaptor<DailyContent> captor = ArgumentCaptor.forClass(DailyContent.class);
        verify(dailyContentRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPayload())
                .containsEntry("text", "Fresh model insight.")
                .containsEntry("fallback", false);
    }

    @Test
    void cacheMiss_modelFailure_persistsFallbackTextFlagged() {
        when(dailyContentRepository.findByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.AI_INSIGHT, TODAY)).thenReturn(Optional.empty());
        when(dailyContentRepository.saveAndFlush(any(DailyContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(openRouterClient.complete(anyString(), anyString()))
                .thenThrow(new IllegalStateException("model unavailable"));

        AiInsightService.Insight insight = service().getOrGenerate(USER_ID, preferences(), coinPrices(), TODAY);

        assertThat(insight.text()).isEqualTo(AiInsightService.FALLBACK_TEXT);
        assertThat(insight.fallback()).isTrue();

        ArgumentCaptor<DailyContent> captor = ArgumentCaptor.forClass(DailyContent.class);
        verify(dailyContentRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPayload()).containsEntry("fallback", true);
    }

    @Test
    void userPromptCarriesProfileAndRealPriceMoves() {
        when(dailyContentRepository.findByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.AI_INSIGHT, TODAY)).thenReturn(Optional.empty());
        when(dailyContentRepository.saveAndFlush(any(DailyContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(openRouterClient.complete(anyString(), anyString())).thenReturn("Insight.");

        service().getOrGenerate(USER_ID, preferences(), coinPrices(), TODAY);

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(openRouterClient).complete(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue())
                .contains("HODLER")
                .contains("MARKET_NEWS")
                .contains("bitcoin, ethereum")
                .contains("bitcoin: 2.34%");
    }
}
