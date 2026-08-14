package com.moveo.aicryptoadvisor.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moveo.aicryptoadvisor.dto.request.PreferencesRequest;
import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import com.moveo.aicryptoadvisor.exception.UnknownCoinIdException;
import com.moveo.aicryptoadvisor.repository.DailyContentRepository;
import com.moveo.aicryptoadvisor.repository.UserPreferencesRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the AI Insight cache-invalidation branch added alongside preference edits: a real
 * profile change must clear today's cached insight so the next dashboard load regenerates it,
 * but a no-op resubmit (or first-time onboarding, where nothing is cached yet) must not.
 */
@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @Mock
    private DailyContentRepository dailyContentRepository;

    private final CoinCatalog coinCatalog = new CoinCatalog(new com.fasterxml.jackson.databind.ObjectMapper());

    private PreferenceService service() {
        return new PreferenceService(preferencesRepository, dailyContentRepository, coinCatalog);
    }

    private static UserPreferences existingPreferences() {
        return new UserPreferences(USER_ID, InvestorType.HODLER, List.of("bitcoin", "ethereum"),
                List.of(ContentType.MARKET_NEWS));
    }

    @Test
    void firstTimeOnboarding_doesNotTouchDailyContent() {
        when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PreferencesRequest request = new PreferencesRequest(
                InvestorType.HODLER, List.of("bitcoin"), List.of(ContentType.FUN));

        service().upsertForUser(USER_ID, request);

        verify(dailyContentRepository, never())
                .deleteByUserIdAndContentTypeAndContentDate(any(), any(), any());
    }

    @Test
    void editWithRealChange_invalidatesTodaysInsight() {
        when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPreferences()));
        when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Different investor type — a genuine profile change.
        PreferencesRequest request = new PreferencesRequest(
                InvestorType.DAY_TRADER, List.of("bitcoin", "ethereum"), List.of(ContentType.MARKET_NEWS));

        service().upsertForUser(USER_ID, request);

        verify(dailyContentRepository).deleteByUserIdAndContentTypeAndContentDate(
                USER_ID, DailyContentType.AI_INSIGHT, LocalDate.now(ZoneOffset.UTC));
    }

    @Test
    void editWithDifferentInterests_invalidatesTodaysInsight() {
        when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPreferences()));
        when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PreferencesRequest request = new PreferencesRequest(
                InvestorType.HODLER, List.of("solana", "dogecoin"), List.of(ContentType.MARKET_NEWS));

        service().upsertForUser(USER_ID, request);

        verify(dailyContentRepository).deleteByUserIdAndContentTypeAndContentDate(
                eq(USER_ID), eq(DailyContentType.AI_INSIGHT), any());
    }

    @Test
    void resubmittingTheSameSetInADifferentOrder_isNotATreatedAsAChange() {
        when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPreferences()));
        when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Same two coins as existingPreferences(), reversed order — not a real change.
        PreferencesRequest request = new PreferencesRequest(
                InvestorType.HODLER, List.of("ethereum", "bitcoin"), List.of(ContentType.MARKET_NEWS));

        service().upsertForUser(USER_ID, request);

        verify(dailyContentRepository, never())
                .deleteByUserIdAndContentTypeAndContentDate(any(), any(), any());
    }

    @Test
    void unknownCoinId_rejectedBeforeTouchingAnyRepository() {
        PreferencesRequest request = new PreferencesRequest(
                InvestorType.HODLER, List.of("not-a-real-coin"), List.of(ContentType.MARKET_NEWS));

        assertThatThrownBy(() -> service().upsertForUser(USER_ID, request))
                .isInstanceOf(UnknownCoinIdException.class);

        verify(dailyContentRepository, never())
                .deleteByUserIdAndContentTypeAndContentDate(any(), any(), any());
    }
}
