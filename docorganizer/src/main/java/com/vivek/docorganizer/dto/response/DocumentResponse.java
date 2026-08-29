package com.vivek.docorganizer.dto.response;

import com.vivek.docorganizer.entity.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public view of a document. The on-disk path and stored filename are intentionally omitted so
 * the storage layout is never exposed to clients.
 */
public record DocumentResponse(
        Long id,
        String name,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        List<String> tags,
        LocalDateTime uploadedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getChecksumSha256(),
                List.copyOf(document.getTags()),
                document.getUploadedAt());
    }
}
