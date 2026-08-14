package com.moveo.aicryptoadvisor.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenRouter chat-completions call behind the AI Insight of the Day (specs.md §7.1).
 * Throws on failure or an empty completion; {@code AiInsightService} owns the fallback text.
 */
@Component
public class OpenRouterClient {

    /**
     * The default free model is a reasoning model whose chain-of-thought counts against this
     * budget before any visible text is produced — 600 leaves headroom for both (verified
     * 2026-08 against real completions of ~250-420 total tokens including reasoning).
     */
    private static final int MAX_TOKENS = 600;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(@JsonProperty("choices") List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(@JsonProperty("message") Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(@JsonProperty("role") String role, @JsonProperty("content") String content) {
    }

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenRouterClient(
            RestClient openRouterRestClient,
            @Value("${app.openrouter.api-key}") String apiKey,
            @Value("${app.openrouter.model}") String model
    ) {
        this.restClient = openRouterRestClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is not configured");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", MAX_TOKENS,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenRouter returned no choices");
        }

        Message message = response.choices().get(0).message();
        String content = message == null ? null : message.content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenRouter returned an empty completion");
        }

        return content.trim();
    }
}
