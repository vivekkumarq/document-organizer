package com.vivek.docorganizer.dto.response;

/** One row of the per-content-type breakdown in the storage stats response. */
public record ContentTypeUsage(String contentType, long fileCount, long bytesUsed) { }
