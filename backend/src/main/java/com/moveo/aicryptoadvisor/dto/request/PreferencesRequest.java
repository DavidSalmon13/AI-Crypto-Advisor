package com.moveo.aicryptoadvisor.dto.request;

import com.moveo.aicryptoadvisor.entity.ContentType;
import com.moveo.aicryptoadvisor.entity.InvestorType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record PreferencesRequest(
        @NotNull InvestorType investorType,
        @NotNull @Size(min = 1, max = 10) @UniqueElements List<String> interests,
        @NotNull @Size(min = 1, max = 4) @UniqueElements List<ContentType> contentTypes
) {
}
