package com.vivek.docorganizer.repository;

import com.vivek.docorganizer.entity.Document;
import com.vivek.docorganizer.service.DocumentSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Criteria-API predicates behind the search endpoint.
 *
 * <p>The owner predicate is always added first and is not derived from anything the client
 * sends, so no combination of filters can reach another account's rows.
 */
public final class DocumentSpecifications {

    private DocumentSpecifications() { }

    public static Specification<Document> forOwner(Long userId, DocumentSearchCriteria criteria) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));

            if (hasText(criteria.filename())) {
                String pattern = "%" + criteria.filename().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            if (hasText(criteria.tag())) {
                // Tags are normalised to lowercase on write, so an exact membership test is enough.
                predicates.add(cb.isMember(criteria.tag().trim().toLowerCase(Locale.ROOT),
                        root.<List<String>>get("tags")));
            }

            if (hasText(criteria.contentType())) {
                String pattern = criteria.contentType().trim().toLowerCase(Locale.ROOT);
                if (pattern.endsWith("/*")) {
                    // "image/*" style prefix match.
                    predicates.add(cb.like(cb.lower(root.get("contentType")),
                            pattern.substring(0, pattern.length() - 1) + "%"));
                } else {
                    predicates.add(cb.equal(cb.lower(root.get("contentType")), pattern));
                }
            }

            if (criteria.uploadedAfter() != null) {
                LocalDateTime from = criteria.uploadedAfter().atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("uploadedAt"), from));
            }

            if (criteria.uploadedBefore() != null) {
                LocalDate before = criteria.uploadedBefore();
                LocalDateTime to = LocalDateTime.of(before, LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("uploadedAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
