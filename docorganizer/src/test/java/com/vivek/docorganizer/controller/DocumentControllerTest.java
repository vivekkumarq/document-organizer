package com.vivek.docorganizer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.vivek.docorganizer.AbstractIntegrationTest;
import com.vivek.docorganizer.entity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------- upload

    @Test
    @DisplayName("upload stores the file, its tags, its size and a SHA-256 checksum")
    void uploadStoresDocument() throws Exception {

        String auth = registerAndAuthorize("upload@example.com");

        MvcResult result = upload(auth, "notes.txt", "text/plain", "hello world", "Work, TAX")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("notes.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.sizeBytes").value(11))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        // SHA-256 of "hello world".
        assertThat(body.get("checksumSha256").asText())
                .isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");

        // Tags are normalised to lowercase and trimmed.
        List<String> tags = objectMapper.convertValue(body.get("tags"), List.class);
        assertThat(tags).containsExactlyInAnyOrder("work", "tax");

        Document stored = documentRepository.findAll().get(0);
        assertThat(Files.exists(Paths.get(stored.getFilePath()))).isTrue();
    }

    @Test
    @DisplayName("a path-traversal filename is reduced to its basename and stays inside the root")
    void uploadSanitisesFilename() throws Exception {

        String auth = registerAndAuthorize("traversal@example.com");

        upload(auth, "../../../../etc/passwd", "text/plain", "not really passwd", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("passwd"));

        Document stored = documentRepository.findAll().get(0);

        Path root = Paths.get("target/test-storage").toAbsolutePath().normalize();

        assertThat(stored.getStoredName()).doesNotContain("..").doesNotContain("/");
        assertThat(Paths.get(stored.getFilePath()).normalize()).startsWith(root);
    }

    @Test
    @DisplayName("a disallowed content type is rejected with 400")
    void uploadRejectsDisallowedContentType() throws Exception {

        String auth = registerAndAuthorize("mime@example.com");

        upload(auth, "payload.exe", "application/x-msdownload", "MZ", null)
                .andExpect(status().isBadRequest());

        assertThat(documentRepository.count()).isZero();
    }

    @Test
    @DisplayName("an empty file is rejected with 400")
    void uploadRejectsEmptyFile() throws Exception {

        String auth = registerAndAuthorize("empty@example.com");

        upload(auth, "empty.txt", "text/plain", "", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a file over app.storage.max-file-size-bytes is rejected with 413")
    void uploadRejectsOversizedFile() throws Exception {

        String auth = registerAndAuthorize("big@example.com");

        // The test profile caps a single file at 2048 bytes.
        upload(auth, "big.txt", "text/plain", "x".repeat(2049), null)
                .andExpect(status().isPayloadTooLarge());

        assertThat(documentRepository.count()).isZero();
    }

    @Test
    @DisplayName("uploading identical bytes twice returns 409 and leaves one copy on disk")
    void duplicateUploadIsRejected() throws Exception {

        String auth = registerAndAuthorize("dupe@example.com");

        upload(auth, "a.txt", "text/plain", "same content", null)
                .andExpect(status().isCreated());

        upload(auth, "b.txt", "text/plain", "same content", null)
                .andExpect(status().isConflict());

        assertThat(documentRepository.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------- quota

    @Test
    @DisplayName("an upload that would exceed the quota is rejected with 413")
    void quotaIsEnforced() throws Exception {

        String auth = registerAndAuthorize("quota@example.com");

        // Test profile: quota 4096 bytes, max single file 2048 bytes.
        upload(auth, "one.txt", "text/plain", "a".repeat(2000), null)
                .andExpect(status().isCreated());

        upload(auth, "two.txt", "text/plain", "b".repeat(2000), null)
                .andExpect(status().isCreated());

        upload(auth, "three.txt", "text/plain", "c".repeat(2000), null)
                .andExpect(status().isPayloadTooLarge());

        assertThat(documentRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("stats reports files, bytes, quota, remainder and a per-type breakdown")
    void statsReportsUsage() throws Exception {

        String auth = registerAndAuthorize("stats@example.com");

        upload(auth, "note.txt", "text/plain", "12345", null).andExpect(status().isCreated());
        upload(auth, "doc.pdf", "application/pdf", "%PDF-1.4", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(2))
                .andExpect(jsonPath("$.bytesUsed").value(13))
                .andExpect(jsonPath("$.quotaBytes").value(4096))
                .andExpect(jsonPath("$.bytesRemaining").value(4083))
                .andExpect(jsonPath("$.byContentType.length()").value(2));
    }

    @Test
    @DisplayName("one user's stats never include another user's files")
    void statsAreOwnerScoped() throws Exception {

        String alice = registerAndAuthorize("alice-stats@example.com");
        String bob = registerAndAuthorize("bob-stats@example.com");

        upload(alice, "alice.txt", "text/plain", "aaaa", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents/stats").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(0))
                .andExpect(jsonPath("$.bytesUsed").value(0));
    }

    // ------------------------------------------------------------- owner scoping

    @Test
    @DisplayName("a user cannot download another user's document")
    void downloadIsOwnerScoped() throws Exception {

        String alice = registerAndAuthorize("alice-dl@example.com");
        String bob = registerAndAuthorize("bob-dl@example.com");

        long id = uploadAndGetId(alice, "private.txt", "text/plain", "alice private data");

        mockMvc.perform(get("/api/documents/{id}/download", id).header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(content().string("alice private data"));

        mockMvc.perform(get("/api/documents/{id}/download", id).header("Authorization", bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/documents/{id}", id).header("Authorization", bob))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("download requires authentication")
    void downloadRequiresToken() throws Exception {

        String alice = registerAndAuthorize("alice-anon@example.com");

        long id = uploadAndGetId(alice, "x.txt", "text/plain", "secret");

        mockMvc.perform(get("/api/documents/{id}/download", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a user cannot delete another user's document")
    void deleteIsOwnerScoped() throws Exception {

        String alice = registerAndAuthorize("alice-del@example.com");
        String bob = registerAndAuthorize("bob-del@example.com");

        long id = uploadAndGetId(alice, "keep.txt", "text/plain", "still here");

        mockMvc.perform(delete("/api/documents/{id}", id).header("Authorization", bob))
                .andExpect(status().isNotFound());

        assertThat(documentRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("delete removes the row and the file on disk")
    void deleteRemovesRowAndFile() throws Exception {

        String auth = registerAndAuthorize("delete@example.com");

        long id = uploadAndGetId(auth, "gone.txt", "text/plain", "delete me");

        Path onDisk = Paths.get(documentRepository.findById(id).orElseThrow().getFilePath());

        assertThat(Files.exists(onDisk)).isTrue();

        mockMvc.perform(delete("/api/documents/{id}", id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        assertThat(documentRepository.findById(id)).isEmpty();
        assertThat(Files.exists(onDisk)).isFalse();
    }

    @Test
    @DisplayName("the list endpoint only ever returns the caller's documents")
    void listIsOwnerScoped() throws Exception {

        String alice = registerAndAuthorize("alice-list@example.com");
        String bob = registerAndAuthorize("bob-list@example.com");

        upload(alice, "alice.txt", "text/plain", "alice", null).andExpect(status().isCreated());
        upload(bob, "bob.txt", "text/plain", "bob", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("bob.txt"));
    }

    // ------------------------------------------------------------- search, filter, paging

    @Test
    @DisplayName("filename filter is a case-insensitive substring match")
    void searchByFilename() throws Exception {

        String auth = registerAndAuthorize("search-name@example.com");

        upload(auth, "Invoice-2026.pdf", "application/pdf", "invoice body", null).andExpect(status().isCreated());
        upload(auth, "holiday-photo.png", "image/png", "png bytes", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents").param("filename", "invoice").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Invoice-2026.pdf"));
    }

    @Test
    @DisplayName("tag filter matches regardless of the case used at upload or query time")
    void searchByTag() throws Exception {

        String auth = registerAndAuthorize("search-tag@example.com");

        upload(auth, "tax.pdf", "application/pdf", "tax body", "Finance,2026").andExpect(status().isCreated());
        upload(auth, "cat.png", "image/png", "cat bytes", "pets").andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents").param("tag", "FINANCE").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("tax.pdf"));

        mockMvc.perform(get("/api/documents").param("tag", "nothing-here").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("content type filter supports an exact value and an image/* prefix")
    void searchByContentType() throws Exception {

        String auth = registerAndAuthorize("search-type@example.com");

        upload(auth, "a.pdf", "application/pdf", "pdf body", null).andExpect(status().isCreated());
        upload(auth, "b.png", "image/png", "png body", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents").param("contentType", "application/pdf")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/documents").param("contentType", "image/*")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("b.png"));
    }

    @Test
    @DisplayName("a date range that excludes today returns nothing, one that includes it returns all")
    void searchByDateRange() throws Exception {

        String auth = registerAndAuthorize("search-date@example.com");

        upload(auth, "today.txt", "text/plain", "today", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents")
                        .param("uploadedAfter", "2000-01-01")
                        .param("uploadedBefore", "2000-12-31")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/documents")
                        .param("uploadedAfter", "2000-01-01")
                        .param("uploadedBefore", "2999-12-31")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("results are paginated and sortable")
    void paginationAndSorting() throws Exception {

        String auth = registerAndAuthorize("paging@example.com");

        upload(auth, "c.txt", "text/plain", "one", null).andExpect(status().isCreated());
        upload(auth, "a.txt", "text/plain", "two", null).andExpect(status().isCreated());
        upload(auth, "b.txt", "text/plain", "three", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents")
                        .param("page", "0").param("size", "2")
                        .param("sort", "name").param("direction", "asc")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("a.txt"))
                .andExpect(jsonPath("$.content[1].name").value("b.txt"));

        mockMvc.perform(get("/api/documents")
                        .param("page", "1").param("size", "2")
                        .param("sort", "name").param("direction", "asc")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].name").value("c.txt"));
    }

    @Test
    @DisplayName("an unknown sort field falls back to uploadedAt instead of failing")
    void unknownSortFieldIsIgnored() throws Exception {

        String auth = registerAndAuthorize("sort@example.com");

        upload(auth, "one.txt", "text/plain", "one", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents").param("sort", "password").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("the tags endpoint lists the caller's distinct tags")
    void tagsEndpoint() throws Exception {

        String auth = registerAndAuthorize("tags@example.com");

        upload(auth, "a.txt", "text/plain", "one", "work,tax").andExpect(status().isCreated());
        upload(auth, "b.txt", "text/plain", "two", "work,personal").andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents/tags").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ------------------------------------------------------------- helpers

    private org.springframework.test.web.servlet.ResultActions upload(String auth,
                                                                      String filename,
                                                                      String contentType,
                                                                      String body,
                                                                      String tags) throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", filename, contentType, body.getBytes(StandardCharsets.UTF_8));

        MockMultipartHttpServletRequestBuilder request =
                multipart("/api/documents/upload").file(file);

        if (tags != null) {
            request.param("tags", tags);
        }

        return mockMvc.perform(request.header("Authorization", auth));
    }

    private long uploadAndGetId(String auth, String filename, String contentType, String body)
            throws Exception {

        MvcResult result = upload(auth, filename, contentType, body, null)
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
