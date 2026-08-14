package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.client.CoinGeckoClient;
import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Live coin prices — deliberately uncached (specs.md §4.3: staleness is undesirable here).
 * A CoinGecko outage yields an empty list rather than a 5xx.
 */
@Service
public class CoinPriceService {

    private static final Logger log = LoggerFactory.getLogger(CoinPriceService.class);

    private final CoinGeckoClient coinGeckoClient;
    private final CoinCatalog coinCatalog;

    public CoinPriceService(CoinGeckoClient coinGeckoClient, CoinCatalog coinCatalog) {
        this.coinGeckoClient = coinGeckoClient;
        this.coinCatalog = coinCatalog;
    }

    public List<DashboardResponse.CoinPrice> getPrices(List<String> coinIds) {
        if (coinIds.isEmpty()) {
            return List.of();
        }

        Map<String, CoinGeckoClient.PriceQuote> quotes;
        try {
            quotes = coinGeckoClient.fetchPrices(coinIds);
        } catch (RuntimeException e) {
            log.warn("CoinGecko price lookup failed for {} — returning no prices", coinIds, e);
            return List.of();
        }

        return coinIds.stream()
                .map(coinId -> toCoinPrice(coinId, quotes.get(coinId)))
                .filter(Objects::nonNull)
                .toList();
    }

    private DashboardResponse.CoinPrice toCoinPrice(String coinId, CoinGeckoClient.PriceQuote quote) {
        if (quote == null || quote.usd() == null) {
            return null;
        }

        // Names/symbols come from the curated catalog, not CoinGecko — one fewer field to trust
        // from an external response, and the catalog is what onboarding validated against.
        return coinCatalog.findById(coinId)
                .map(coin -> new DashboardResponse.CoinPrice(
                        coin.id(),
                        coin.symbol(),
                        coin.name(),
                        quote.usd().setScale(2, RoundingMode.HALF_UP),
                        round2(quote.usd24hChange())))
                .orElse(null);
    }

    private static BigDecimal round2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
