package com.vivek.docorganizer.service;

import com.vivek.docorganizer.config.StorageProperties;
import com.vivek.docorganizer.dto.response.ContentTypeUsage;
import com.vivek.docorganizer.dto.response.StorageStatsResponse;
import com.vivek.docorganizer.entity.Document;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.exception.BadRequestException;
import com.vivek.docorganizer.exception.ConflictException;
import com.vivek.docorganizer.exception.NotFoundException;
import com.vivek.docorganizer.exception.QuotaExceededException;
import com.vivek.docorganizer.repository.DocumentRepository;
import com.vivek.docorganizer.repository.DocumentSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Document lifecycle: upload, search, download and delete.
 *
 * <p>Every read and write is scoped by the owner's user id, which the caller obtains from the
 * verified JWT rather than from a request parameter.
 */
@Service
public class DocumentService {

    /** Upper bound on how many tags a single document may carry. */
    private static final int MAX_TAGS = 10;

    /** Upper bound on the length of a single tag. Matches the column definition. */
    private static final int MAX_TAG_LENGTH = 40;

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    public DocumentService(DocumentRepository documentRepository,
                           FileStorageService fileStorageService,
                           StorageProperties storageProperties) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
    }

    // ---------------------------------------------------------------- upload

    @Transactional
    public Document upload(MultipartFile file, Collection<String> rawTags, User owner) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was supplied");
        }

        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new QuotaExceededException("File exceeds the maximum upload size of "
                    + storageProperties.getMaxFileSizeBytes() + " bytes");
        }

        String contentType = normaliseContentType(file.getContentType());

        List<String> allowed = storageProperties.getAllowedContentTypes();

        if (allowed != null && !allowed.isEmpty()
                && allowed.stream().noneMatch(type -> type.equalsIgnoreCase(contentType))) {
            throw new BadRequestException("Content type " + contentType + " is not allowed");
        }

        long quota = quotaFor(owner);
        long used = documentRepository.sumSizeBytesByUserId(owner.getId());

        if (used + file.getSize() > quota) {
            throw new QuotaExceededException("Storage quota exceeded: "
                    + used + " of " + quota + " bytes used, upload is " + file.getSize() + " bytes");
        }

        Set<String> tags = normaliseTags(rawTags);

        String displayName = fileStorageService.sanitizeFilename(file.getOriginalFilename());
        String storedName = fileStorageService.buildStoredName(displayName);

        String checksum = fileStorageService.store(file, storedName);

        Optional<Document> duplicate =
                documentRepository.findByUserIdAndChecksumSha256(owner.getId(), checksum);

        if (duplicate.isPresent()) {
            // Roll the file back off disk before failing so no orphan is left behind.
            fileStorageService.delete(storedName);
            throw new ConflictException("An identical file is already stored as \""
                    + duplicate.get().getName() + "\"");
        }

        Document document = new Document();
        document.setName(displayName);
        document.setStoredName(storedName);
        document.setFilePath(fileStorageService.resolve(storedName).toString());
        document.setContentType(contentType);
        document.setSizeBytes(file.getSize());
        document.setChecksumSha256(checksum);
        document.setTags(tags);
        document.setUserId(owner.getId());
        document.setUploadedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public Page<Document> search(User owner, DocumentSearchCriteria criteria, Pageable pageable) {
        return documentRepository.findAll(
                DocumentSpecifications.forOwner(owner.getId(), criteria), pageable);
    }

    @Transactional(readOnly = true)
    public List<Document> listAll(User owner) {
        return documentRepository.findByUserId(owner.getId());
    }

    @Transactional(readOnly = true)
    public List<String> listTags(User owner) {
        return documentRepository.findDistinctTagsByUserId(owner.getId());
    }

    /**
     * Loads a document, scoped to its owner.
     *
     * <p>A document belonging to somebody else is reported as 404 rather than 403 so the
     * endpoint does not confirm that the id exists.
     */
    @Transactional(readOnly = true)
    public Document getOwned(Long id, User owner) {
        return documentRepository.findByIdAndUserId(id, owner.getId())
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }

    /** Resolves the file behind an owned document, verifying it is still on disk. */
    @Transactional(readOnly = true)
    public Path resolveFile(Document document) {

        Path path = fileStorageService.resolve(document.getStoredName());

        if (!Files.isReadable(path)) {
            throw new NotFoundException("Stored file for document " + document.getId() + " is missing");
        }

        return path;
    }

    // ---------------------------------------------------------------- delete

    @Transactional
    public void delete(Long id, User owner) {

        Document document = getOwned(id, owner);

        documentRepository.delete(document);

        fileStorageService.delete(document.getStoredName());
    }

    // ---------------------------------------------------------------- stats

    @Transactional(readOnly = true)
    public StorageStatsResponse stats(User owner) {

        long totalFiles = documentRepository.countByUserId(owner.getId());
        long bytesUsed = documentRepository.sumSizeBytesByUserId(owner.getId());
        long quota = quotaFor(owner);

        List<ContentTypeUsage> breakdown = new ArrayList<>();

        for (Object[] row : documentRepository.storageBreakdownByUserId(owner.getId())) {
            breakdown.add(new ContentTypeUsage(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue()));
        }

        double percentUsed = quota == 0 ? 0d
                : Math.round((bytesUsed * 10_000d) / quota) / 100d;

        return new StorageStatsResponse(
                totalFiles,
                bytesUsed,
                quota,
                Math.max(0, quota - bytesUsed),
                percentUsed,
                breakdown);
    }

    public long quotaFor(User user) {
        return user.getStorageQuotaBytes() != null
                ? user.getStorageQuotaBytes()
                : storageProperties.getQuotaBytes();
    }

    // ---------------------------------------------------------------- helpers

    private static String normaliseContentType(String contentType) {

        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }

        // Drop any parameters such as "; charset=UTF-8".
        int semicolon = contentType.indexOf(';');

        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;

        return base.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> normaliseTags(Collection<String> rawTags) {

        Set<String> tags = new LinkedHashSet<>();

        if (rawTags == null) {
            return tags;
        }

        for (String raw : rawTags) {

            if (raw == null) {
                continue;
            }

            // A single "a,b,c" parameter is as acceptable as repeated tag parameters.
            for (String part : raw.split(",")) {

                String tag = part.trim().toLowerCase(Locale.ROOT);

                if (tag.isEmpty()) {
                    continue;
                }

                if (tag.length() > MAX_TAG_LENGTH) {
                    throw new BadRequestException(
                            "Tag \"" + tag + "\" exceeds " + MAX_TAG_LENGTH + " characters");
                }

                tags.add(tag);
            }
        }

        if (tags.size() > MAX_TAGS) {
            throw new BadRequestException("At most " + MAX_TAGS + " tags are allowed per document");
        }

        return tags;
    }
}
