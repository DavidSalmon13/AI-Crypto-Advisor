package com.moveo.aicryptoadvisor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "investor_type", nullable = false)
    private InvestorType investorType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> interests;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_types", nullable = false)
    private List<ContentType> contentTypes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected UserPreferences() {
    }

    public UserPreferences(UUID userId, InvestorType investorType, List<String> interests, List<ContentType> contentTypes) {
        this.userId = userId;
        this.investorType = investorType;
        this.interests = interests;
        this.contentTypes = contentTypes;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void replaceWith(InvestorType investorType, List<String> interests, List<ContentType> contentTypes) {
        this.investorType = investorType;
        this.interests = interests;
        this.contentTypes = contentTypes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public InvestorType getInvestorType() {
        return investorType;
    }

    public List<String> getInterests() {
        return interests;
    }

    public List<ContentType> getContentTypes() {
        return contentTypes;
    }
}
