package com.moveo.aicryptoadvisor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveo.aicryptoadvisor.dto.request.FeedbackRequest;
import com.moveo.aicryptoadvisor.dto.request.PreferencesRequest;
import com.moveo.aicryptoadvisor.dto.request.RegisterRequest;
import com.moveo.aicryptoadvisor.dto.response.AuthResponse;
import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import com.moveo.aicryptoadvisor.dto.response.FeedbackResponse;
import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import com.moveo.aicryptoadvisor.entity.ItemType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Votes exercised through the real endpoints, against real {@code itemRef} values taken from
 * a live dashboard response — the round trip specs.md §4.4 describes, including the
 * "reload and the vote is still there" behaviour via {@code userVote}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class FeedbackControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String token;

    private String onboardedUser() {
        if (token != null) {
            return token;
        }
        String email = "vote-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> register = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "Vote Tester", "Str0ngPass"), AuthResponse.class);
        token = register.getBody().token();

        restTemplate.exchange(
                "/api/preferences", HttpMethod.PUT,
                new HttpEntity<>(new PreferencesRequest(
                        InvestorType.DAY_TRADER,
                        List.of("bitcoin"),
                        List.of(ContentType.MARKET_NEWS)), headers()),
                Void.class);
        return token;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private DashboardResponse dashboard() {
        return restTemplate.exchange(
                "/api/dashboard", HttpMethod.GET, new HttpEntity<>(headers()), DashboardResponse.class).getBody();
    }

    private ResponseEntity<FeedbackResponse> vote(ItemType itemType, String itemRef, int value) {
        return restTemplate.exchange(
                "/api/feedback", HttpMethod.POST,
                new HttpEntity<>(new FeedbackRequest(itemType, itemRef, value), headers()),
                FeedbackResponse.class);
    }

    private ResponseEntity<Void> retract(String itemType, String itemRef) {
        return restTemplate.exchange(
                "/api/feedback/" + itemType + "/" + itemRef, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);
    }

    @Test
    void voteFlipsInPlaceAndSurvivesAReload() {
        onboardedUser();
        String newsRef = dashboard().marketNews().get(0).id();

        ResponseEntity<FeedbackResponse> up = vote(ItemType.NEWS, newsRef, 1);
        assertThat(up.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(up.getBody().vote()).isEqualTo(1);

        // Re-voting must flip the same row, not create a second one — same id proves it.
        ResponseEntity<FeedbackResponse> down = vote(ItemType.NEWS, newsRef, -1);
        assertThat(down.getBody().id()).isEqualTo(up.getBody().id());
        assertThat(down.getBody().vote()).isEqualTo(-1);

        DashboardResponse reloaded = dashboard();
        assertThat(reloaded.marketNews())
                .filteredOn(item -> item.id().equals(newsRef))
                .singleElement()
                .satisfies(item -> assertThat(item.userVote()).isEqualTo(-1));
    }

    @Test
    void retractIsIdempotent() {
        onboardedUser();
        DashboardResponse before = dashboard();
        String memeRef = before.meme().id().toString();

        assertThat(vote(ItemType.MEME, memeRef, 1).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retract("MEME", memeRef).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Second delete: still 204, not 404 (specs.md §4.4).
        assertThat(retract("MEME", memeRef).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(dashboard().meme().userVote()).isNull();
    }

    @Test
    void insightAndMemeVotesAreTrackedSeparately() {
        onboardedUser();
        DashboardResponse before = dashboard();

        vote(ItemType.AI_INSIGHT, before.aiInsight().id().toString(), 1);
        vote(ItemType.MEME, before.meme().id().toString(), -1);

        DashboardResponse after = dashboard();
        assertThat(after.aiInsight().userVote()).isEqualTo(1);
        assertThat(after.meme().userVote()).isEqualTo(-1);
    }

    @Test
    void invalidVoteValueIsRejected() {
        onboardedUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/feedback", HttpMethod.POST,
                new HttpEntity<>(new FeedbackRequest(ItemType.NEWS, "cc-1", 0), headers()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }

    @Test
    void unknownItemTypeInPathIsABadRequestNotAServerError() {
        onboardedUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/feedback/BOGUS/cc-1", HttpMethod.DELETE, new HttpEntity<>(headers()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }

    /**
     * Uses the JDK client rather than TestRestTemplate: HttpURLConnection sends a request
     * body in streaming mode and then refuses to surface a 401 at all, throwing
     * {@code HttpRetryException} instead. That is a legacy-client quirk, not API behaviour —
     * axios and fetch both see the 401 normally.
     */
    @Test
    void votingRequiresAuthentication() throws Exception {
        HttpResponse<String> response = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .build()
                .send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/feedback"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        "{\"itemType\":\"NEWS\",\"itemRef\":\"cc-1\",\"vote\":1}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.body()).contains("UNAUTHORIZED");
    }
}
