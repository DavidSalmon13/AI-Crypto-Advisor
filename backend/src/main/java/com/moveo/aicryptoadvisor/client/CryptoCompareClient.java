package com.moveo.aicryptoadvisor.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * CryptoCompare (CoinDesk Data) news API — the aggregated market-news source (specs.md §4.3).
 * Article ids are prefixed {@code cc-} on the way out: they become {@code feedback.item_ref}
 * values, so the prefix keeps them unambiguous if the provider is ever swapped again.
 *
 * <p>Throws on any failure; {@code NewsService} owns the static-fallback policy.
 */
@Component
public class CryptoCompareClient {

    public static final String ID_PREFIX = "cc-";

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewsListResponse(@JsonProperty("Data") List<Article> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Article(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("published_on") Long publishedOn,
            @JsonProperty("source_info") SourceInfo sourceInfo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SourceInfo(@JsonProperty("name") String name) {
    }

    private final RestClient restClient;
    private final String apiKey;

    public CryptoCompareClient(
            RestClient cryptoCompareRestClient,
            @Value("${app.cryptocompare.api-key}") String apiKey
    ) {
        this.restClient = cryptoCompareRestClient;
        this.apiKey = apiKey;
    }

    /** @return latest English articles, newest first, as returned by the provider. */
    public List<NewsArticle> fetchLatestNews() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("CRYPTOCOMPARE_API_KEY is not configured");
        }

        NewsListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/v2/news/")
                        .queryParam("lang", "EN")
                        .build())
                .header("Authorization", "Apikey " + apiKey)
                .retrieve()
                .body(NewsListResponse.class);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("CryptoCompare returned no news payload");
        }

        return response.data().stream()
                .filter(article -> article.id() != null && article.title() != null && article.url() != null)
                .map(CryptoCompareClient::toNewsArticle)
                .toList();
    }

    private static NewsArticle toNewsArticle(Article article) {
        return new NewsArticle(
                ID_PREFIX + article.id(),
                article.title(),
                article.url(),
                article.sourceInfo() == null ? "Unknown" : Objects.requireNonNullElse(article.sourceInfo().name(), "Unknown"),
                article.publishedOn() == null ? Instant.EPOCH : Instant.ofEpochSecond(article.publishedOn()));
    }
}
