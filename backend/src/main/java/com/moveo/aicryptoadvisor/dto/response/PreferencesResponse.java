package com.moveo.aicryptoadvisor.dto.response;

import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import com.moveo.aicryptoadvisor.entity.UserPreferences;
import java.util.List;

public record PreferencesResponse(
        InvestorType investorType,
        List<String> interests,
        List<ContentType> contentTypes
) {

    public static PreferencesResponse from(UserPreferences preferences) {
        return new PreferencesResponse(
                preferences.getInvestorType(),
                preferences.getInterests(),
                preferences.getContentTypes());
    }
}
