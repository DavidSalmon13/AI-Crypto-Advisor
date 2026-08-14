package com.moveo.aicryptoadvisor.client;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Crypto news via public RSS feeds. CryptoPanic, CryptoCompare/CoinDesk Data, and
 * CoinGecko's News endpoint all turned out to require a paid subscription (verified
 * 2026-08) — publisher RSS feeds are what these sites already syndicate for free, with
 * no key and no rate-limited tier to get pulled out from under this app later.
 *
 * <p>Article ids are a hash of the article URL, prefixed {@code rss-}: feeds don't carry
 * a canonical numeric id the way the old provider APIs did, but the hash is stable across
 * requests, which is what {@code feedback.item_ref} needs.
 *
 * <p>Throws only if every configured feed fails; {@code NewsService} owns the
 * static-fallback policy for that case. A single feed failing is not fatal — the other
 * feed's articles are still returned.
 */
@Component
public class RssNewsClient {

    private static final Logger log = LoggerFactory.getLogger(RssNewsClient.class);

    public static final String ID_PREFIX = "rss-";

    private record Feed(String source, String url) {
    }

    private static final List<Feed> FEEDS = List.of(
            new Feed("Cointelegraph", "https://cointelegraph.com/rss"),
            new Feed("Decrypt", "https://decrypt.co/feed")
    );

    private final RestClient restClient;

    public RssNewsClient(RestClient newsRestClient) {
        this.restClient = newsRestClient;
    }

    /** @return articles from all configured feeds, newest first. */
    public List<NewsArticle> fetchLatestNews() {
        List<NewsArticle> articles = new ArrayList<>();
        for (Feed feed : FEEDS) {
            articles.addAll(fetchFeed(feed));
        }
        if (articles.isEmpty()) {
            throw new IllegalStateException("All RSS news feeds returned zero articles");
        }
        return articles.stream()
                .sorted(Comparator.comparing(NewsArticle::publishedAt).reversed())
                .toList();
    }

    private List<NewsArticle> fetchFeed(Feed feed) {
        try {
            byte[] body = restClient.get().uri(URI.create(feed.url())).retrieve().body(byte[].class);
            if (body == null || body.length == 0) {
                return List.of();
            }
            try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
                SyndFeed syndFeed = new SyndFeedInput().build(reader);
                return syndFeed.getEntries().stream()
                        .filter(entry -> entry.getTitle() != null && entry.getLink() != null)
                        .map(entry -> toNewsArticle(feed.source(), entry))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("RSS feed fetch failed for {} ({}) — skipping this source", feed.source(), feed.url(), e);
            return List.of();
        }
    }

    private static NewsArticle toNewsArticle(String source, SyndEntry entry) {
        Instant publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant()
                : entry.getUpdatedDate() != null
                        ? entry.getUpdatedDate().toInstant()
                        : Instant.now();
        return new NewsArticle(
                ID_PREFIX + sha256(entry.getLink()),
                entry.getTitle().trim(),
                entry.getLink(),
                source,
                publishedAt);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
