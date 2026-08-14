package com.moveo.aicryptoadvisor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One generated content item per (user, type, UTC day) — the lazily-populated cache
 * behind AI Insight and Meme (specs.md §7.2). The DB unique constraint on
 * (user_id, content_type, content_date) is what makes concurrent generation safe.
 */
@Entity
@Table(name = "daily_content")
public class DailyContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private DailyContentType contentType;

    @Column(name = "content_date", nullable = false)
    private LocalDate contentDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    /**
     * Assigned in Java rather than left to the DB default, because it is returned straight
     * back to the client as {@code aiInsight.generatedAt} on the generating request — a
     * DB-side default would not be visible without an extra round trip.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DailyContent() {
    }

    public DailyContent(UUID userId, DailyContentType contentType, LocalDate contentDate, Map<String, Object> payload) {
        this.userId = userId;
        this.contentType = contentType;
        this.contentDate = contentDate;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public DailyContentType getContentType() {
        return contentType;
    }

    public LocalDate getContentDate() {
        return contentDate;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
