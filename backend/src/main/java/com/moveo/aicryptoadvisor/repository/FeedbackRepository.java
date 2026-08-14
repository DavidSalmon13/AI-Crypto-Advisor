package com.moveo.aicryptoadvisor.repository;

import com.moveo.aicryptoadvisor.entity.Feedback;
import com.moveo.aicryptoadvisor.entity.ItemType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    /** Votes for exactly the items on one rendered dashboard, rather than a user's whole history. */
    List<Feedback> findByUserIdAndItemRefIn(UUID userId, Collection<String> itemRefs);

    Optional<Feedback> findByUserIdAndItemTypeAndItemRef(UUID userId, ItemType itemType, String itemRef);
}
