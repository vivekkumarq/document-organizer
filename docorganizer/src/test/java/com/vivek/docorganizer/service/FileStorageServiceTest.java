package com.vivek.docorganizer.service;

import com.vivek.docorganizer.config.StorageProperties;
import com.vivek.docorganizer.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the filesystem layer: filename sanitisation, path containment and checksums. */
class FileStorageServiceTest {

    private FileStorageService service;

    @BeforeEach
    void setUp() throws IOException {

        StorageProperties properties = new StorageProperties();
        properties.setDir("target/unit-test-storage");

        service = new FileStorageService(properties);
        service.init();
    }

    @ParameterizedTest(name = "\"{0}\" becomes \"{1}\"")
    @CsvSource({
            "report.pdf, report.pdf",
            "../../etc/passwd, passwd",
            "..\\..\\windows\\system32\\cmd.exe, cmd.exe",
            "/absolute/path/file.txt, file.txt",
            "C:\\Users\\vivek\\secret.doc, secret.doc",
            "'my report (final).pdf', my report _final_.pdf",
            "'  spaced.txt  ', spaced.txt",
            ".hidden, hidden"
    })
    @DisplayName("sanitizeFilename strips directories and unsafe characters")
    void sanitizeFilename(String input, String expected) {
        assertThat(service.sanitizeFilename(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("a filename that sanitises to nothing is rejected")
    void unusableFilenamesAreRejected() {

        assertThatThrownBy(() -> service.sanitizeFilename(".."))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.sanitizeFilename("../"))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.sanitizeFilename(null))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.sanitizeFilename("   "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("the generated stored name is unique and directory-free")
    void storedNamesAreUniqueAndFlat() {

        String first = service.buildStoredName("report.pdf");
        String second = service.buildStoredName("report.pdf");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).endsWith("_report.pdf").doesNotContain("/").doesNotContain("\\");
    }

    @Test
    @DisplayName("resolve refuses any name that would escape the storage root")
    void resolveRefusesEscapes() {

        assertThatThrownBy(() -> service.resolve("../outside.txt"))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.resolve("a/../../outside.txt"))
                .isInstanceOf(BadRequestException.class);

        // Compared as strings: AssertJ's Path.startsWith resolves real paths, which would
        // require the file to already exist on disk.
        assertThat(service.resolve("inside.txt").toString())
                .startsWith(service.getRoot().toString());
    }

    @Test
    @DisplayName("store writes the bytes and returns their SHA-256")
    void storeWritesAndChecksums() throws IOException {

        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello world".getBytes(StandardCharsets.UTF_8));

        String storedName = service.buildStoredName("hello.txt");

        String checksum = service.store(file, storedName);

        assertThat(checksum)
                .isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");

        Path written = service.resolve(storedName);

        assertThat(Files.readString(written)).isEqualTo("hello world");

        assertThat(service.delete(storedName)).isTrue();
        assertThat(Files.exists(written)).isFalse();
        assertThat(service.delete(storedName)).isFalse();
    }
}
