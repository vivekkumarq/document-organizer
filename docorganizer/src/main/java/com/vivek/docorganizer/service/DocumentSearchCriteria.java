package com.vivek.docorganizer.service;

import java.time.LocalDate;

/**
 * Filters accepted by {@code GET /api/documents}. Every field is optional; a null field means
 * "do not constrain on this".
 */
public record DocumentSearchCriteria(
        String filename,
        String tag,
        String contentType,
        LocalDate uploadedAfter,
        LocalDate uploadedBefore) {

    public static DocumentSearchCriteria empty() {
        return new DocumentSearchCriteria(null, null, null, null, null);
    }
}
