package com.vivek.docorganizer.controller;

import com.vivek.docorganizer.dto.response.DocumentResponse;
import com.vivek.docorganizer.dto.response.PageResponse;
import com.vivek.docorganizer.dto.response.StorageStatsResponse;
import com.vivek.docorganizer.entity.Document;
import com.vivek.docorganizer.entity.User;
import com.vivek.docorganizer.security.CurrentUser;
import com.vivek.docorganizer.service.DocumentSearchCriteria;
import com.vivek.docorganizer.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Upload, search, download and delete documents")
public class DocumentController {

    /** Fields a client is allowed to sort by. Anything else is rejected. */
    private static final Set<String> SORTABLE =
            Set.of("uploadedAt", "name", "sizeBytes", "contentType", "id");

    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentService documentService;
    private final CurrentUser currentUser;

    public DocumentController(DocumentService documentService, CurrentUser currentUser) {
        this.documentService = documentService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "List the caller's documents",
            description = "Paginated, sortable and filterable. All filters are optional and are "
                    + "combined with AND. Results are always restricted to the caller.")
    public PageResponse<DocumentResponse> list(
            @Parameter(description = "Case-insensitive substring match on the filename")
            @RequestParam(required = false) String filename,
            @Parameter(description = "Exact tag match, case-insensitive")
            @RequestParam(required = false) String tag,
            @Parameter(description = "Exact content type, or a prefix such as image/*")
            @RequestParam(required = false) String contentType,
            @Parameter(description = "Only documents uploaded on or after this date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedAfter,
            @Parameter(description = "Only documents uploaded on or before this date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedBefore,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, capped at 100")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "One of uploadedAt, name, sizeBytes, contentType, id")
            @RequestParam(defaultValue = "uploadedAt") String sort,
            @Parameter(description = "asc or desc")
            @RequestParam(defaultValue = "desc") String direction) {

        User owner = currentUser.require();

        DocumentSearchCriteria criteria = new DocumentSearchCriteria(
                filename, tag, contentType, uploadedAfter, uploadedBefore);

        Page<Document> result = documentService.search(owner, criteria, toPageable(page, size, sort, direction));

        return PageResponse.from(result, DocumentResponse::from);
    }

    @GetMapping("/tags")
    @Operation(summary = "List the distinct tags used by the caller")
    public List<String> tags() {
        return documentService.listTags(currentUser.require());
    }

    @GetMapping("/stats")
    @Operation(summary = "Storage usage and quota for the caller",
            description = "Total files, bytes used, quota, remaining bytes and a breakdown by content type.")
    public StorageStatsResponse stats() {
        return documentService.stats(currentUser.require());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document",
            description = "Validates size and content type, sanitises the filename, stores a "
                    + "SHA-256 checksum and rejects a file the caller has already uploaded.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stored"),
            @ApiResponse(responseCode = "400", description = "Empty file, bad filename, disallowed content type or invalid tags", content = @Content),
            @ApiResponse(responseCode = "409", description = "Identical file already stored", content = @Content),
            @ApiResponse(responseCode = "413", description = "File too large or quota exceeded", content = @Content)
    })
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Repeatable, or a single comma-separated value")
            @RequestParam(name = "tags", required = false) List<String> tags) throws IOException {

        User owner = currentUser.require();

        Document document = documentService.upload(file, tags, owner);

        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one document's metadata")
    @ApiResponse(responseCode = "404", description = "No such document owned by the caller", content = @Content)
    public DocumentResponse get(@PathVariable Long id) {
        return DocumentResponse.from(documentService.getOwned(id, currentUser.require()));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the file behind a document",
            description = "Owner-scoped: a document belonging to another account returns 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File stream"),
            @ApiResponse(responseCode = "404", description = "No such document owned by the caller", content = @Content)
    })
    public ResponseEntity<Resource> download(@PathVariable Long id) {

        User owner = currentUser.require();

        Document document = documentService.getOwned(id, owner);

        Path path = documentService.resolveFile(document);

        String encodedName = encodeFilename(document.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CONTENT_TYPE, document.getContentType())
                .contentLength(document.getSizeBytes())
                .body(new PathResource(path));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and its file on disk",
            description = "Owner-scoped: a document belonging to another account returns 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "No such document owned by the caller", content = @Content)
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        documentService.delete(id, currentUser.require());

        return ResponseEntity.noContent().build();
    }

    private static Pageable toPageable(int page, int size, String sort, String direction) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        String property = SORTABLE.contains(sort) ? sort : "uploadedAt";

        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(dir, property));
    }

    /** Keeps the Content-Disposition header well-formed for names with quotes or non-ASCII. */
    private static String encodeFilename(String name) {

        String cleaned = name.replace("\"", "").replace("\r", "").replace("\n", "");

        return java.net.URLEncoder.encode(cleaned, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
