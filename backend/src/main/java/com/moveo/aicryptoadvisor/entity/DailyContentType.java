package com.moveo.aicryptoadvisor.entity;

/**
 * What kind of once-per-user-per-day content a {@link DailyContent} row holds.
 * Distinct from {@link ContentType}, which is the onboarding content-style preference.
 */
public enum DailyContentType {
    AI_INSIGHT,
    MEME
}
