package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.config.ArborEvidenceProperties;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local-disk evidence store (default / H2). Layout:
 * {@code {storageRoot}/{tenantId}/{evidenceId}/v{n}-{uuid}-{filename}}.
 */
@Component
@ConditionalOnProperty(name = "arbor.evidence.storage", havingValue = "local", matchIfMissing = true)
public class LocalEvidenceFileStore implements EvidenceFileStore {

    private final ArborEvidenceProperties properties;
    private final Set<String> allowedExt;

    public LocalEvidenceFileStore(ArborEvidenceProperties properties) {
        this.properties = properties;
        this.allowedExt = Arrays.stream(properties.getAllowedExtensions().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String backend() {
        return "local";
    }

    @Override
    public StoredFile store(long evidenceId, int versionNumber, MultipartFile file) {
        validate(file);
        try {
            return storeBytes(evidenceId, versionNumber, file.getOriginalFilename(),
                    file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }

    @Override
    public StoredFile storeBytes(long evidenceId, int versionNumber,
                                 String originalFilename, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (bytes.length > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File exceeds max size of " + (properties.getMaxFileBytes() / (1024 * 1024)) + "MB");
        }
        String original = safeFilename(originalFilename);
        String ext = extension(original);
        if (!allowedExt.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Permitted: " + properties.getAllowedExtensions());
        }
        EvidenceContentTypeSupport.assertCompatible(ext, contentType);
        EvidenceMagicBytesSupport.assertMatchesExtension(ext, bytes);
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant required");
        }
        String key = tenantId + "/" + evidenceId + "/v" + versionNumber + "-"
                + UUID.randomUUID().toString().substring(0, 8) + "-" + original;
        Path dest = root().resolve(key).normalize();
        if (!dest.startsWith(root())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        }
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
        String ct = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        return new StoredFile(key, original, ct, bytes.length);
    }

    @Override
    public Resource open(String storageKey) {
        Path path = root().resolve(storageKey).normalize();
        if (!path.startsWith(root()) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new FileSystemResource(path);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File exceeds max size of " + (properties.getMaxFileBytes() / (1024 * 1024)) + "MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extension(name);
        if (!allowedExt.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Permitted: " + properties.getAllowedExtensions());
        }
    }

    private Path root() {
        return Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "upload.bin";
        }
        String base = Path.of(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        return base.isBlank() ? "upload.bin" : base;
    }
}
