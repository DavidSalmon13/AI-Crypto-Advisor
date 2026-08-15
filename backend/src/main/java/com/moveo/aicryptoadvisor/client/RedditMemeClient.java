package com.moveo.aicryptoadvisor.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Crypto memes via Reddit's public, unauthenticated JSON listing endpoint — no API key or
 * OAuth app registration required (specs.md §7.3 decision log: Reddit was originally
 * rejected as "fragile, rate-limited"; revisited once a static-pool fallback made that
 * fragility harmless to the dashboard).
 *
 * <p>Only image posts above a minimum score are kept, filtering out text posts, videos,
 * galleries the dashboard can't render inline, and low-effort spam. Never throws — any
 * failure (network, rate limit, malformed response) is logged and an empty list is
 * returned; {@code MemeService} owns the static-fallback policy for that case.
 */
@Component
public class RedditMemeClient {

    private static final Logger log = LoggerFactory.getLogger(RedditMemeClient.class);

    public static final String ID_PREFIX = "reddit-";

    private static final int MIN_UPVOTES = 200;
    private static final List<String> IMAGE_SUFFIXES = List.of(".jpg", ".jpeg", ".png", ".gif");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String listingUrl;

    public RedditMemeClient(
            RestClient redditRestClient,
            ObjectMapper objectMapper,
            @Value("${app.reddit.listing-url}") String listingUrl
    ) {
        this.restClient = redditRestClient;
        this.objectMapper = objectMapper;
        this.listingUrl = listingUrl;
    }

    /** @return usable image-post memes, newest/top first, or an empty list if the feed is unavailable. */
    public List<Meme> fetchTopMemes() {
        try {
            String body = restClient.get()
                    .uri(URI.create(listingUrl))
                    .header(HttpHeaders.USER_AGENT, "ai-crypto-advisor/1.0 (dashboard meme feed)")
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            return parseListing(body);
        } catch (Exception e) {
            log.warn("Reddit meme feed fetch failed — falling back to the static meme pool", e);
            return List.of();
        }
    }

    private List<Meme> parseListing(String body) throws Exception {
        JsonNode children = objectMapper.readTree(body).path("data").path("children");
        List<Meme> memes = new ArrayList<>();
        for (JsonNode child : children) {
            JsonNode post = child.path("data");
            if (isUsableImagePost(post)) {
                memes.add(new Meme(
                        ID_PREFIX + post.path("id").asText(),
                        post.path("url").asText(),
                        post.path("title").asText("Crypto meme")));
            }
        }
        return memes;
    }

    private static boolean isUsableImagePost(JsonNode post) {
        if (post.path("over_18").asBoolean(false)) {
            return false;
        }
        if (post.path("ups").asInt(0) < MIN_UPVOTES) {
            return false;
        }
        String url = post.path("url").asText("").toLowerCase(Locale.ROOT);
        return IMAGE_SUFFIXES.stream().anyMatch(url::endsWith);
    }
}
