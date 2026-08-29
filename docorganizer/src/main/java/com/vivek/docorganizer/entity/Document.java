package com.vivek.docorganizer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_user_id", columnList = "user_id"),
        @Index(name = "idx_documents_checksum", columnList = "user_id, checksum_sha256")
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original (sanitised) filename shown to the user and used for the download header. */
    @Column(nullable = false)
    private String name;

    /** Opaque name the file is written under on disk. Never derived verbatim from user input. */
    @Column(name = "stored_name", nullable = false)
    private String storedName;

    /** Absolute path of the stored file. */
    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Lowercase hex SHA-256 of the file contents, used for duplicate detection. */
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "document_tags",
            joinColumns = @JoinColumn(name = "document_id"),
            indexes = @Index(name = "idx_document_tags_tag", columnList = "tag"))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public Document() { }

    public Long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getStoredName() { return storedName; }

    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getFilePath() { return filePath; }

    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContentType() { return contentType; }

    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }

    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getChecksumSha256() { return checksumSha256; }

    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public Set<String> getTags() { return tags; }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
