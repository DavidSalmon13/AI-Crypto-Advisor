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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A 👍/👎 vote on one dashboard item. Read-only in Phase 5 (used to populate
 * {@code userVote}); the write endpoints arrive in Phase 6.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Column(name = "item_ref", nullable = false)
    private String itemRef;

    @Column(nullable = false)
    private short vote;

    @Column(name = "item_date", nullable = false)
    private LocalDate itemDate;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected Feedback() {
    }

    public Feedback(UUID userId, ItemType itemType, String itemRef, short vote, LocalDate itemDate) {
        this.userId = userId;
        this.itemType = itemType;
        this.itemRef = itemRef;
        this.vote = vote;
        this.itemDate = itemDate;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void changeVote(short vote) {
        this.vote = vote;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getItemRef() {
        return itemRef;
    }

    public short getVote() {
        return vote;
    }

    public LocalDate getItemDate() {
        return itemDate;
    }
}
