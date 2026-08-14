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
}
