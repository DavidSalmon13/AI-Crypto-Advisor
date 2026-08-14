package com.moveo.aicryptoadvisor.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * CoinGecko public price API (specs.md §4.3 — no key required, no caching: prices must be live).
 * Throws on any failure; {@code CoinPriceService} decides the degradation policy.
 */
@Component
public class CoinGeckoClient {

    /** A single coin's quote as returned by {@code /simple/price}. */
    public record PriceQuote(
            @JsonProperty("usd") BigDecimal usd,
            @JsonProperty("usd_24h_change") BigDecimal usd24hChange
    ) {
    }

    private static final ParameterizedTypeReference<Map<String, PriceQuote>> QUOTES_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public CoinGeckoClient(RestClient coinGeckoRestClient) {
        this.restClient = coinGeckoRestClient;
    }

    /** @return quotes keyed by CoinGecko coin id; ids CoinGecko doesn't know are simply absent. */
    public Map<String, PriceQuote> fetchPrices(List<String> coinIds) {
        if (coinIds.isEmpty()) {
            return Map.of();
        }

        Map<String, PriceQuote> quotes = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", String.join(",", coinIds))
                        .queryParam("vs_currencies", "usd")
                        .queryParam("include_24hr_change", "true")
                        .build())
                .retrieve()
                .body(QUOTES_TYPE);

        return quotes == null ? Map.of() : quotes;
    }
}
