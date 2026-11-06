package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.config.ArborEvidenceProperties;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Azure Blob Storage backend (UAE North in prod). Activate with
 * {@code arbor.evidence.storage=blob} and a connection string.
 */
@Component
@ConditionalOnProperty(name = "arbor.evidence.storage", havingValue = "blob")
public class AzureBlobEvidenceFileStore implements EvidenceFileStore {

    private final ArborEvidenceProperties properties;
    private final Set<String> allowedExt;
    private final BlobContainerClient container;

    public AzureBlobEvidenceFileStore(ArborEvidenceProperties properties) {
        this.properties = properties;
        this.allowedExt = Arrays.stream(properties.getAllowedExtensions().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        String cs = properties.getBlobConnectionString();
        String containerName = properties.getBlobContainer();
        if (cs == null || cs.isBlank()) {
            throw new IllegalStateException(
                    "arbor.evidence.storage=blob requires arbor.evidence.blob-connection-string "
                            + "(ARBOR_EVIDENCE_BLOB_CONNECTION_STRING)");
        }
        if (containerName == null || containerName.isBlank()) {
            throw new IllegalStateException("arbor.evidence.blob-container is required when storage=blob");
        }
        this.container = new BlobServiceClientBuilder()
                .connectionString(cs)
                .buildClient()
                .getBlobContainerClient(containerName);
        if (!this.container.exists()) {
            this.container.create();
        }
    }

    @Override
    public String backend() {
        return "blob";
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
        String ct = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        try {
            BlobClient blob = container.getBlobClient(key);
            blob.upload(new ByteArrayInputStream(bytes), bytes.length, true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType(ct));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store blob", e);
        }
        return new StoredFile(key, original, ct, bytes.length);
    }

    @Override
    public Resource open(String storageKey) {
        BlobClient blob = container.getBlobClient(storageKey);
        if (!blob.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        InputStream in = blob.openInputStream();
        return new InputStreamResource(in) {
            @Override
            public String getFilename() {
                int slash = storageKey.lastIndexOf('/');
                return slash >= 0 ? storageKey.substring(slash + 1) : storageKey;
            }

            @Override
            public long contentLength() {
                return blob.getProperties().getBlobSize();
            }
        };
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
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return base.isBlank() ? "upload.bin" : base;
    }
}
