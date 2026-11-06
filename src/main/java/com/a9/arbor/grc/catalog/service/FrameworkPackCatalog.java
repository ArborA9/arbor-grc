package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.config.ArborFrameworkProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Discovers framework Excel packs from the configured directory when it exists,
 * otherwise the packs copied into the JAR at {@code classpath:framework-packs/}.
 */
@Component
public class FrameworkPackCatalog {

    static final String CLASSPATH_PATTERN = "classpath*:framework-packs/**/*.xlsx";
    static final String CLASSPATH_MARKER = "framework-packs/";

    public record PackFile(
            String moduleFolder,
            String relativePath,
            String filename,
            Path absolutePath,
            Resource resource,
            boolean importable,
            String skipReason
    ) {
        public byte[] readBytes() throws IOException {
            if (absolutePath != null) {
                return Files.readAllBytes(absolutePath);
            }
            if (resource == null) {
                throw new IOException("No file or classpath resource for " + relativePath);
            }
            try (InputStream in = resource.getInputStream()) {
                return in.readAllBytes();
            }
        }
    }

    private final ArborFrameworkProperties properties;
    private final ResourcePatternResolver resourceResolver;

    public FrameworkPackCatalog(ArborFrameworkProperties properties) {
        this.properties = properties;
        this.resourceResolver = new PathMatchingResourcePatternResolver();
    }

    public Path resolveRoot() {
        return Paths.get(properties.getDir()).toAbsolutePath().normalize();
    }

    public boolean usingFilesystem() {
        return Files.isDirectory(resolveRoot());
    }

    public String statusDirectory() {
        if (usingFilesystem()) {
            return resolveRoot().toString();
        }
        return "bundled (classpath:framework-packs)";
    }

    public List<String> listModuleFolders() {
        return listAllPacks().stream()
                .map(PackFile::moduleFolder)
                .filter(name -> !"(root)".equals(name))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<PackFile> listAllPacks() {
        if (usingFilesystem()) {
            return listFilesystemPacks();
        }
        return listClasspathPacks();
    }

    public List<PackFile> listModulePacks(String moduleFolder) {
        String wanted = moduleFolder == null ? "" : moduleFolder.trim();
        return listAllPacks().stream()
                .filter(p -> p.moduleFolder().equalsIgnoreCase(wanted))
                .toList();
    }

    public PackFile requirePack(String relativePath) {
        String rel = relativePath == null ? "" : relativePath.replace('\\', '/').trim();
        return listAllPacks().stream()
                .filter(p -> p.relativePath().equalsIgnoreCase(rel)
                        || p.filename().equalsIgnoreCase(rel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Framework pack not found: " + relativePath));
    }

    private List<PackFile> listFilesystemPacks() {
        Path root = resolveRoot();
        List<PackFile> out = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                    .filter(p -> !p.getFileName().toString().startsWith("~$"))
                    .sorted(Comparator.comparing(p -> root.relativize(p).toString(),
                            String.CASE_INSENSITIVE_ORDER))
                    .forEach(p -> out.add(toPackFile(root, p)));
        } catch (IOException ignored) {
            return List.of();
        }
        return out;
    }

    private List<PackFile> listClasspathPacks() {
        Resource[] found;
        try {
            found = resourceResolver.getResources(CLASSPATH_PATTERN);
        } catch (IOException e) {
            return List.of();
        }
        List<PackFile> out = new ArrayList<>();
        for (Resource resource : found) {
            if (!resource.exists()) {
                continue;
            }
            String filename = resource.getFilename();
            if (filename == null || filename.startsWith("~$")
                    || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                continue;
            }
            String relativePath = classpathRelativePath(resource, filename);
            String moduleFolder = moduleFolderOf(relativePath);
            String skip = skipReason(relativePath, filename);
            out.add(new PackFile(moduleFolder, relativePath, filename, null, resource,
                    skip == null, skip));
        }
        out.sort(Comparator.comparing(PackFile::relativePath, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static String classpathRelativePath(Resource resource, String filename) {
        try {
            String uri = resource.getURI().toString().replace('\\', '/');
            int marker = uri.lastIndexOf(CLASSPATH_MARKER);
            if (marker >= 0) {
                String rel = uri.substring(marker + CLASSPATH_MARKER.length());
                return URLDecoder.decode(rel, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // fall through
        }
        return filename;
    }

    private static String moduleFolderOf(String relativePath) {
        int slash = relativePath.indexOf('/');
        return slash > 0 ? relativePath.substring(0, slash) : "(root)";
    }

    private PackFile toPackFile(Path root, Path file) {
        Path rel = root.relativize(file);
        String relativePath = rel.toString().replace('\\', '/');
        String moduleFolder = rel.getNameCount() > 1
                ? rel.getName(0).toString()
                : "(root)";
        String filename = file.getFileName().toString();
        String skip = skipReason(relativePath, filename);
        return new PackFile(moduleFolder, relativePath, filename, file, null,
                skip == null, skip);
    }

    /**
     * Non-library files under module folders (BIA forms, activity samples, maturity workbooks).
     */
    static String skipReason(String relativePath, String filename) {
        String path = relativePath.toLowerCase(Locale.ROOT);
        String name = filename.toLowerCase(Locale.ROOT);
        if (path.contains("/pims activity/") || path.contains("/activity/")) {
            return "Activity / sample workbook: not a framework library";
        }
        if (name.contains("business impact") || name.contains("bia ")
                || name.startsWith("ssd - bia") || name.contains("bia final")) {
            return "BCMS BIA form: out of framework import (Activity later)";
        }
        if (name.contains("risk asses") || name.contains("risk assess")) {
            return "Risk assessment form: not a framework library";
        }
        if (name.contains("gap_assessment") || name.contains("gap assessment")
                || name.contains("maturity_workbook")) {
            return "Gap / maturity process workbook: reference only, not a library";
        }
        if (name.contains("dpia") || name.startsWith("ropa ")) {
            return "Privacy activity sample: not a framework library";
        }
        return null;
    }
}
