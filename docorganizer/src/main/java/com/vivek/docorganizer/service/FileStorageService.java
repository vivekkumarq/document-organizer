package com.vivek.docorganizer.service;

import com.vivek.docorganizer.config.StorageProperties;
import com.vivek.docorganizer.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Everything that touches the filesystem.
 *
 * <p>Two properties matter for safety here:
 * <ul>
 *   <li>the name a file is stored under is generated, not taken from the client, and</li>
 *   <li>every resolved path is checked to still live under the storage root before use,
 *       so a crafted filename such as {@code ../../etc/passwd} cannot escape it.</li>
 * </ul>
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Anything outside this set is replaced with an underscore in the display name. */
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^A-Za-z0-9._ -]");

    private static final int MAX_NAME_LENGTH = 120;

    private final StorageProperties properties;

    private Path root;

    public FileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws IOException {

        this.root = Paths.get(properties.getDir()).toAbsolutePath().normalize();

        Files.createDirectories(root);

        log.info("Document storage root: {}", root);
    }

    public Path getRoot() {
        return root;
    }

    /**
     * Strips any directory component and unsafe characters from a client-supplied filename.
     *
     * <p>Handles both separators explicitly because a POSIX server can still receive a Windows
     * path from a browser and vice versa.
     */
    public String sanitizeFilename(String original) {

        if (original == null || original.isBlank()) {
            throw new BadRequestException("Uploaded file must have a filename");
        }

        String name = original.trim();

        // Keep only the last path segment, whichever separator the client used.
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }

        // Strip control characters, then anything not in the safe set.
        name = name.replaceAll("[\\p{Cntrl}]", "");
        name = UNSAFE_CHARS.matcher(name).replaceAll("_");

        // Leading dots would produce hidden files; a bare ".." must not survive.
        while (name.startsWith(".")) {
            name = name.substring(1);
        }

        name = name.trim();

        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(name.length() - MAX_NAME_LENGTH);
        }

        if (name.isBlank()) {
            throw new BadRequestException("Filename is not usable after sanitisation");
        }

        return name;
    }

    /** Builds the opaque on-disk name: a random UUID plus the sanitised display name. */
    public String buildStoredName(String sanitizedName) {
        return UUID.randomUUID().toString().replace("-", "")
                + "_" + sanitizedName.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /**
     * Resolves a stored name against the storage root and refuses anything that would land
     * outside it.
     */
    public Path resolve(String storedName) {

        Path candidate = root.resolve(storedName).normalize();

        if (!candidate.startsWith(root)) {
            throw new BadRequestException("Resolved path escapes the storage root");
        }

        return candidate;
    }

    /** Writes the upload to disk and returns its SHA-256 checksum as lowercase hex. */
    public String store(MultipartFile file, String storedName) throws IOException {

        Path target = resolve(storedName);

        MessageDigest digest = newSha256();

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        try (InputStream in = Files.newInputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        return toHex(digest.digest());
    }

    /** Deletes a stored file. Returns false when the file was already gone. */
    public boolean delete(String storedName) {

        try {
            return Files.deleteIfExists(resolve(storedName));
        } catch (IOException ex) {
            log.warn("Could not delete stored file {}: {}", storedName, ex.getMessage());
            return false;
        }
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }

    private static String toHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }

        return sb.toString();
    }
}
