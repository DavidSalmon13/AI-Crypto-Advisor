package com.moveo.aicryptoadvisor.service;

import com.moveo.aicryptoadvisor.entity.DailyContent;
import com.moveo.aicryptoadvisor.entity.DailyContentType;
import com.moveo.aicryptoadvisor.repository.DailyContentRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * The read-or-generate-once-per-day pattern shared by AI Insight and Meme (specs.md §7.2).
 *
 * <p>Deliberately <em>not</em> {@code @Transactional}: each repository call runs in its own
 * transaction, so when two concurrent first-requests race, the loser's failed insert rolls
 * back on its own and the follow-up read still works. Wrapping the whole method in one
 * transaction would mark it rollback-only and make that recovery read fail.
 */
@Component
public class DailyContentCache {

    private static final Logger log = LoggerFactory.getLogger(DailyContentCache.class);

    private final DailyContentRepository repository;

    public DailyContentCache(DailyContentRepository repository) {
        this.repository = repository;
    }

    /**
     * @param payloadSupplier invoked only on a cache miss — the expensive generation step.
     * @return the row for (user, type, date), whether it already existed, was just written,
     *         or was written concurrently by another request.
     */
    public DailyContent getOrCreate(
            UUID userId,
            DailyContentType contentType,
            LocalDate contentDate,
            Supplier<Map<String, Object>> payloadSupplier
    ) {
        Optional<DailyContent> existing =
                repository.findByUserIdAndContentTypeAndContentDate(userId, contentType, contentDate);
        if (existing.isPresent()) {
            return existing.get();
        }

        DailyContent candidate = new DailyContent(userId, contentType, contentDate, payloadSupplier.get());
        try {
            return repository.saveAndFlush(candidate);
        } catch (DataIntegrityViolationException e) {
            // Lost the race against a concurrent first request for the same user/day; the
            // unique constraint on (user_id, content_type, content_date) is what caught it.
            log.debug("Concurrent daily-content insert for user {} type {} — reading the winner", userId, contentType);
            return repository.findByUserIdAndContentTypeAndContentDate(userId, contentType, contentDate)
                    .orElseThrow(() -> e);
        }
    }
}
