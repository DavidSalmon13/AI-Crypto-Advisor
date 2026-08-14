package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.dto.request.PreferencesRequest;
import com.moveo.aicryptoadvisor.dto.response.PreferencesResponse;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import com.moveo.aicryptoadvisor.exception.PreferencesNotSetException;
import com.moveo.aicryptoadvisor.exception.UnknownCoinIdException;
import com.moveo.aicryptoadvisor.repository.DailyContentRepository;
import com.moveo.aicryptoadvisor.repository.UserPreferencesRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private final UserPreferencesRepository preferencesRepository;
    private final DailyContentRepository dailyContentRepository;
    private final CoinCatalog coinCatalog;

    public PreferenceService(
            UserPreferencesRepository preferencesRepository,
            DailyContentRepository dailyContentRepository,
            CoinCatalog coinCatalog
    ) {
        this.preferencesRepository = preferencesRepository;
        this.dailyContentRepository = dailyContentRepository;
        this.coinCatalog = coinCatalog;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse getForUser(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(PreferencesResponse::from)
                .orElseThrow(PreferencesNotSetException::new);
    }

    @Transactional
    public PreferencesResponse upsertForUser(UUID userId, PreferencesRequest request) {
        for (String coinId : request.interests()) {
            if (!coinCatalog.containsId(coinId)) {
                throw new UnknownCoinIdException(coinId);
            }
        }

        Optional<UserPreferences> existing = preferencesRepository.findByUserId(userId);

        // Evaluated before replaceWith() mutates the same entity in place, since after that
        // point `existing`'s getters would already reflect the new values.
        boolean profileChanged = existing.map(prefs -> preferencesChanged(prefs, request)).orElse(false);

        UserPreferences preferences = existing
                .map(prefs -> {
                    prefs.replaceWith(request.investorType(), request.interests(), request.contentTypes());
                    return prefs;
                })
                .orElseGet(() -> new UserPreferences(
                        userId, request.investorType(), request.interests(), request.contentTypes()));

        PreferencesResponse response = PreferencesResponse.from(preferencesRepository.save(preferences));

        if (profileChanged) {
            // The AI Insight prompt is built from investorType/interests/contentTypes
            // (specs.md §7.1), so a profile edit invalidates today's cached one — without
            // this, the dashboard would keep serving an insight generated under the old
            // profile until the next UTC day (specs.md §7.2's once-per-day cache is otherwise
            // unaware that the input it was generated from just changed).
            dailyContentRepository.deleteByUserIdAndContentTypeAndContentDate(
                    userId, DailyContentType.AI_INSIGHT, LocalDate.now(ZoneOffset.UTC));
        }

        return response;
    }

    /** Order-insensitive: re-submitting the same set in a different click order isn't a change. */
    private static boolean preferencesChanged(UserPreferences existing, PreferencesRequest request) {
        return existing.getInvestorType() != request.investorType()
                || !new HashSet<>(existing.getInterests()).equals(new HashSet<>(request.interests()))
                || !new HashSet<>(existing.getContentTypes()).equals(new HashSet<>(request.contentTypes()));
    }
}
