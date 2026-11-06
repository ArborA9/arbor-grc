package com.a9.arbor.grc.modules.policy.service;

import com.a9.arbor.grc.config.ArborEvidenceProperties;
import com.a9.arbor.grc.tenancy.TenantContext;
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

/** Local-disk policy file store under {@code {storageRoot}/policy/{tenantId}/...}. */
@Component
public class PolicyFileStore {

    public record StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {
    }

    private final ArborEvidenceProperties properties;
    private final Set<String> allowedExt;

    public PolicyFileStore(ArborEvidenceProperties properties) {
        this.properties = properties;
        this.allowedExt = Arrays.stream(properties.getAllowedExtensions().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public StoredFile store(long policyId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File exceeds max size of " + (properties.getMaxFileBytes() / (1024 * 1024)) + "MB");
        }
        String original = safeFilename(file.getOriginalFilename());
        String ext = extension(original);
        if (!allowedExt.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Permitted: " + properties.getAllowedExtensions());
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant required");
        }
        String key = "policy/" + tenantId + "/" + policyId + "/"
                + UUID.randomUUID().toString().substring(0, 8) + "-" + original;
        Path dest = root().resolve(key).normalize();
        if (!dest.startsWith(root())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path");
        }
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
        String ct = file.getContentType();
        return new StoredFile(key, original, ct != null ? ct : "application/octet-stream", file.getSize());
    }

    public Resource load(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file");
        }
        Path path = root().resolve(storageKey).normalize();
        if (!path.startsWith(root()) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new FileSystemResource(path);
    }

    private Path root() {
        return Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
    }

    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file.bin";
        }
        String base = Path.of(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        return base.isBlank() ? "file.bin" : base;
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
