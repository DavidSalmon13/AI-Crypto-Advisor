package com.moveo.aicryptoadvisor.dto.response;

import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import com.moveo.aicryptoadvisor.service.CoinCatalog;
import java.util.List;

public record PreferencesOptionsResponse(
        List<CoinCatalog.Coin> coins,
        List<InvestorType> investorTypes,
        List<ContentType> contentTypes
) {
}
