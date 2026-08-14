package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.dto.request.PreferencesRequest;
import com.moveo.aicryptoadvisor.dto.response.PreferencesResponse;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import com.moveo.aicryptoadvisor.exception.PreferencesNotSetException;
import com.moveo.aicryptoadvisor.exception.UnknownCoinIdException;
import com.moveo.aicryptoadvisor.repository.UserPreferencesRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private final UserPreferencesRepository preferencesRepository;
    private final CoinCatalog coinCatalog;

    public PreferenceService(UserPreferencesRepository preferencesRepository, CoinCatalog coinCatalog) {
        this.preferencesRepository = preferencesRepository;
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

        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .map(existing -> {
                    existing.replaceWith(request.investorType(), request.interests(), request.contentTypes());
                    return existing;
                })
                .orElseGet(() -> new UserPreferences(
                        userId, request.investorType(), request.interests(), request.contentTypes()));

        return PreferencesResponse.from(preferencesRepository.save(preferences));
    }
}
