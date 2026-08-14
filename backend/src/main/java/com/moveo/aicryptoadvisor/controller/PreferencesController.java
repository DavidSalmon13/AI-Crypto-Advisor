package com.moveo.aicryptoadvisor.controller;

import com.moveo.aicryptoadvisor.dto.request.PreferencesRequest;
import com.moveo.aicryptoadvisor.dto.response.PreferencesOptionsResponse;
import com.moveo.aicryptoadvisor.dto.response.PreferencesResponse;
import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import com.moveo.aicryptoadvisor.entity.User;
import com.moveo.aicryptoadvisor.service.CoinCatalog;
import com.moveo.aicryptoadvisor.service.PreferenceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    private final PreferenceService preferenceService;
    private final CoinCatalog coinCatalog;

    public PreferencesController(PreferenceService preferenceService, CoinCatalog coinCatalog) {
        this.preferenceService = preferenceService;
        this.coinCatalog = coinCatalog;
    }

    @GetMapping("/options")
    public PreferencesOptionsResponse getOptions() {
        return new PreferencesOptionsResponse(
                coinCatalog.getCoins(),
                List.of(InvestorType.values()),
                List.of(ContentType.values()));
    }

    @GetMapping
    public PreferencesResponse getPreferences(@AuthenticationPrincipal User user) {
        return preferenceService.getForUser(user.getId());
    }

    @PutMapping
    public PreferencesResponse putPreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PreferencesRequest request
    ) {
        return preferenceService.upsertForUser(user.getId(), request);
    }
}
