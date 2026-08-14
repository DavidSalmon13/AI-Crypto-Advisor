package com.moveo.aicryptoadvisor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The curated 30-coin onboarding list (specs.md §4.2), loaded once at startup
 * from data/coins.json — read-only reference data, deliberately not a DB table.
 */
@Component
public class CoinCatalog {

    public record Coin(String id, String symbol, String name) {
    }

    private final List<Coin> coins;
    private final Set<String> coinIds;

    public CoinCatalog(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("data/coins.json").getInputStream()) {
            this.coins = List.copyOf(objectMapper.readValue(in, new TypeReference<List<Coin>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load data/coins.json", e);
        }
        this.coinIds = coins.stream().map(Coin::id).collect(Collectors.toUnmodifiableSet());
    }

    public List<Coin> getCoins() {
        return coins;
    }

    public boolean containsId(String coinId) {
        return coinIds.contains(coinId);
    }
}
