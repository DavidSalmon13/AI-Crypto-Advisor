package com.moveo.aicryptoadvisor.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * One {@link RestClient} per outbound integration, each pinned to its own base URL and
 * timeout budget (specs.md §4.3/§7.1: 5s for CoinGecko and CryptoCompare, 10s for
 * OpenRouter). Timeouts are the whole point of these beans — an unbounded external call
 * would stall the aggregating dashboard request.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient coinGeckoRestClient(
            @Value("${app.coingecko.base-url}") String baseUrl,
            @Value("${app.coingecko.timeout-seconds}") int timeoutSeconds
    ) {
        return build(baseUrl, timeoutSeconds);
    }

    @Bean
    public RestClient cryptoCompareRestClient(
            @Value("${app.cryptocompare.base-url}") String baseUrl,
            @Value("${app.cryptocompare.timeout-seconds}") int timeoutSeconds
    ) {
        return build(baseUrl, timeoutSeconds);
    }

    @Bean
    public RestClient openRouterRestClient(
            @Value("${app.openrouter.base-url}") String baseUrl,
            @Value("${app.openrouter.timeout-seconds}") int timeoutSeconds
    ) {
        return build(baseUrl, timeoutSeconds);
    }

    private static RestClient build(String baseUrl, int timeoutSeconds) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeoutSeconds))
                .build();
    }

    private static ClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }
}
