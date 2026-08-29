package com.vivek.docorganizer.dto.response;

import java.util.List;

/** Returned by GET /api/documents/stats. */
public record StorageStatsResponse(
        long totalFiles,
        long bytesUsed,
        long quotaBytes,
        long bytesRemaining,
        double percentUsed,
        List<ContentTypeUsage> byContentType) { }
