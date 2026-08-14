package com.moveo.aicryptoadvisor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveo.aicryptoadvisor.dto.request.PreferencesRequest;
import com.moveo.aicryptoadvisor.dto.request.RegisterRequest;
import com.moveo.aicryptoadvisor.dto.response.AuthResponse;
import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end pass over the aggregating endpoint. The test environment has no external API
 * keys and no outbound network, which makes this the graceful-degradation test too: every
 * section must still render from its fallback rather than surfacing a 5xx (specs.md §4.3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class DashboardControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String registerAndOnboard() {
        String email = "dash-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> register = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "Dash Tester", "Str0ngPass"), AuthResponse.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String token = register.getBody().token();

        PreferencesRequest preferences = new PreferencesRequest(
                InvestorType.HODLER,
                List.of("bitcoin", "ethereum"),
                List.of(ContentType.MARKET_NEWS, ContentType.FUN));
        ResponseEntity<Void> saved = restTemplate.exchange(
                "/api/preferences", HttpMethod.PUT, new HttpEntity<>(preferences, authHeaders(token)), Void.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);

        return token;
    }

    private static HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<DashboardResponse> getDashboard(String token) {
        return restTemplate.exchange(
                "/api/dashboard", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), DashboardResponse.class);
    }

    @Test
    void dashboard_returnsAllFourSections_evenWithoutExternalApis() {
        String token = registerAndOnboard();

        ResponseEntity<DashboardResponse> response = getDashboard(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DashboardResponse body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.date()).isEqualTo(LocalDate.now(ZoneOffset.UTC));

        // News degrades to the static file, and every article still carries a votable id.
        assertThat(body.marketNews()).isNotEmpty().hasSizeLessThanOrEqualTo(10);
        assertThat(body.marketNews()).allSatisfy(item -> {
            assertThat(item.id()).isNotBlank();
            assertThat(item.title()).isNotBlank();
            assertThat(item.userVote()).isNull();
        });

        assertThat(body.aiInsight()).isNotNull();
        assertThat(body.aiInsight().id()).isNotNull();
        assertThat(body.aiInsight().text()).isNotBlank();
        assertThat(body.aiInsight().generatedAt()).isNotNull();
        assertThat(body.aiInsight().userVote()).isNull();

        assertThat(body.meme()).isNotNull();
        assertThat(body.meme().id()).isNotNull();
        assertThat(body.meme().imageUrl()).isNotBlank();
        assertThat(body.meme().caption()).isNotBlank();
        assertThat(body.meme().userVote()).isNull();
    }

    @Test
    void dailyContentIsStableAcrossCallsOnTheSameDay() {
        String token = registerAndOnboard();

        DashboardResponse first = getDashboard(token).getBody();
        DashboardResponse second = getDashboard(token).getBody();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.aiInsight().id()).isEqualTo(first.aiInsight().id());
        assertThat(second.aiInsight().text()).isEqualTo(first.aiInsight().text());
        assertThat(second.meme().id()).isEqualTo(first.meme().id());
        assertThat(second.meme().caption()).isEqualTo(first.meme().caption());
    }

    @Test
    void dashboard_withoutOnboarding_returns404() {
        String email = "no-prefs-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> register = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "No Prefs", "Str0ngPass"), AuthResponse.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/dashboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders(register.getBody().token())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PREFERENCES_NOT_SET");
    }

    @Test
    void dashboard_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/dashboard", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
