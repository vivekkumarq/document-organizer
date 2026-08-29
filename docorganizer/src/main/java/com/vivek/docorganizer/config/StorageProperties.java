package com.vivek.docorganizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Storage tuning knobs, all overridable through environment variables.
 *
 * <pre>
 * app.storage.dir                    -&gt; STORAGE_DIR
 * app.storage.quota-bytes            -&gt; STORAGE_QUOTA_BYTES
 * app.storage.max-file-size-bytes    -&gt; STORAGE_MAX_FILE_SIZE_BYTES
 * app.storage.allowed-content-types  -&gt; STORAGE_ALLOWED_CONTENT_TYPES
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Root directory documents are written to. Relative paths resolve against the working directory. */
    private String dir = "storage";

    /** Default per-user quota in bytes. Individual users may override it. */
    private long quotaBytes = 104_857_600L; // 100 MiB

    /** Largest single upload accepted, in bytes. */
    private long maxFileSizeBytes = 10_485_760L; // 10 MiB

    /** Content types accepted at upload time. An empty list means "accept anything". */
    private List<String> allowedContentTypes = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "text/plain",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public String getDir() { return dir; }

    public void setDir(String dir) { this.dir = dir; }

    public long getQuotaBytes() { return quotaBytes; }

    public void setQuotaBytes(long quotaBytes) { this.quotaBytes = quotaBytes; }

    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

    public List<String> getAllowedContentTypes() { return allowedContentTypes; }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
