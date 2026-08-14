package com.moveo.aicryptoadvisor.repository;

import com.moveo.aicryptoadvisor.entity.DailyContent;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyContentRepository extends JpaRepository<DailyContent, UUID> {

    Optional<DailyContent> findByUserIdAndContentTypeAndContentDate(
            UUID userId, DailyContentType contentType, LocalDate contentDate);

    /**
     * Invalidates today's cached AI Insight after a preference edit changes what the prompt
     * should say (specs.md §4.2), so the next dashboard load regenerates it instead of serving
     * a stale insight built from the old profile until the next UTC day.
     */
    void deleteByUserIdAndContentTypeAndContentDate(
            UUID userId, DailyContentType contentType, LocalDate contentDate);
}
