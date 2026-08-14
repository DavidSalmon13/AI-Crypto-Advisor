package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.aicryptoadvisor.client.CryptoCompareClient;
import com.moveo.aicryptoadvisor.client.NewsArticle;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A news-provider outage must degrade to the static file rather than failing the dashboard
 * (specs.md §4.3). Caching itself is proxy-level and so is out of scope here.
 */
@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private CryptoCompareClient cryptoCompareClient;

    private NewsService service() {
        return new NewsService(cryptoCompareClient, new ObjectMapper().findAndRegisterModules());
    }

    private static List<NewsArticle> articles(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new NewsArticle(
                        "cc-" + i, "Headline " + i, "https://example.test/" + i, "CoinDesk", Instant.EPOCH))
                .toList();
    }

    @Test
    void providerSuccess_returnsAtMostTenLiveArticles() {
        when(cryptoCompareClient.fetchLatestNews()).thenReturn(articles(25));

        NewsService.NewsResult result = service().getTopNews();

        assertThat(result.fallback()).isFalse();
        assertThat(result.articles()).hasSize(10);
        assertThat(result.articles().get(0).id()).isEqualTo("cc-1");
    }

    @Test
    void providerFailure_servesStaticFallback() {
        when(cryptoCompareClient.fetchLatestNews()).thenThrow(new IllegalStateException("provider down"));

        NewsService.NewsResult result = service().getTopNews();

        assertThat(result.fallback()).isTrue();
        assertThat(result.articles()).hasSize(10);
        assertThat(result.articles()).allSatisfy(article -> {
            assertThat(article.id()).startsWith("fallback-");
            assertThat(article.title()).isNotBlank();
            assertThat(article.url()).isNotBlank();
            assertThat(article.publishedAt()).isNotNull();
        });
    }

    @Test
    void providerReturnsNothing_servesStaticFallback() {
        when(cryptoCompareClient.fetchLatestNews()).thenReturn(List.of());

        NewsService.NewsResult result = service().getTopNews();

        assertThat(result.fallback()).isTrue();
        assertThat(result.articles()).isNotEmpty();
    }
}
