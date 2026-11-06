package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkBundleFileResult;
import com.a9.arbor.grc.catalog.web.dto.FrameworkBundleImportResultResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkBundleStatusResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkImportResultResponse;
import com.a9.arbor.grc.config.ArborFrameworkProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Folder-based import scopes for Framework Import V2: one framework, one module, or all.
 */
@Service
public class FrameworkBundleImportService {

    public enum Scope {
        FRAMEWORK, MODULE, ALL
    }

    private final ArborFrameworkProperties properties;
    private final FrameworkPackCatalog catalog;
    private final FrameworkLibraryService frameworkLibraryService;

    public FrameworkBundleImportService(ArborFrameworkProperties properties,
                                        FrameworkPackCatalog catalog,
                                        FrameworkLibraryService frameworkLibraryService) {
        this.properties = properties;
        this.catalog = catalog;
        this.frameworkLibraryService = frameworkLibraryService;
    }

    public FrameworkBundleStatusResponse status() {
        boolean present = catalog.usingFilesystem() || !catalog.listAllPacks().isEmpty();
        List<FrameworkPackCatalog.PackFile> packs = catalog.listAllPacks();
        List<FrameworkBundleStatusResponse.PackEntry> entries = packs.stream()
                .filter(FrameworkPackCatalog.PackFile::importable)
                .map(p -> new FrameworkBundleStatusResponse.PackEntry(
                        p.moduleFolder(), p.relativePath(), p.filename(), true))
                .toList();
        List<String> skipped = packs.stream()
                .filter(p -> !p.importable())
                .map(p -> p.relativePath() + ": " + p.skipReason())
                .toList();
        return new FrameworkBundleStatusResponse(
                catalog.statusDirectory(),
                present,
                catalog.listModuleFolders(),
                entries,
                skipped);
    }

    public FrameworkBundleImportResultResponse importBundle(Scope scope,
                                                            String moduleFolder,
                                                            String relativePath,
                                                            boolean dryRun) {
        List<FrameworkPackCatalog.PackFile> selected = switch (scope) {
            case ALL -> catalog.listAllPacks().stream()
                    .filter(FrameworkPackCatalog.PackFile::importable)
                    .toList();
            case MODULE -> {
                if (moduleFolder == null || moduleFolder.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "moduleFolder is required for MODULE scope");
                }
                yield catalog.listModulePacks(moduleFolder).stream()
                        .filter(FrameworkPackCatalog.PackFile::importable)
                        .toList();
            }
            case FRAMEWORK -> {
                if (relativePath == null || relativePath.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "relativePath is required for FRAMEWORK scope");
                }
                FrameworkPackCatalog.PackFile pack;
                try {
                    pack = catalog.requirePack(relativePath);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
                }
                if (!pack.importable()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Pack is not importable: " + pack.skipReason());
                }
                yield List.of(pack);
            }
        };

        if (selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No importable framework packs for scope " + scope
                            + (moduleFolder != null ? " module=" + moduleFolder : "")
                            + ". Check arbor.frameworks.dir=" + properties.getDir());
        }

        List<FrameworkBundleFileResult> files = new ArrayList<>();
        int step = 1;
        int imported = 0;
        int skipped = 0;
        int errors = 0;
        for (FrameworkPackCatalog.PackFile pack : selected) {
            try {
                byte[] bytes = pack.readBytes();
                FrameworkImportResultResponse result = frameworkLibraryService.importExcel(
                        null, null, bytes, pack.filename(), dryRun);
                String message = summarize(result);
                files.add(new FrameworkBundleFileResult(
                        step++, pack.moduleFolder(), pack.relativePath(), pack.filename(),
                        dryRun ? "DRY_RUN" : "OK", message, result, List.of()));
                imported++;
            } catch (ResponseStatusException e) {
                String detail = e.getReason() != null ? e.getReason() : e.getMessage();
                files.add(new FrameworkBundleFileResult(
                        step++, pack.moduleFolder(), pack.relativePath(), pack.filename(),
                        "ERROR", detail, null, List.of(detail)));
                errors++;
            } catch (IOException e) {
                String detail = "Could not read file: " + e.getMessage();
                files.add(new FrameworkBundleFileResult(
                        step++, pack.moduleFolder(), pack.relativePath(), pack.filename(),
                        "ERROR", detail, null, List.of(detail)));
                errors++;
            } catch (RuntimeException e) {
                String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                files.add(new FrameworkBundleFileResult(
                        step++, pack.moduleFolder(), pack.relativePath(), pack.filename(),
                        "ERROR", detail, null, List.of(detail)));
                errors++;
            }
        }

        // Also report catalog skip list entries when importing ALL (visibility).
        if (scope == Scope.ALL) {
            for (FrameworkPackCatalog.PackFile pack : catalog.listAllPacks()) {
                if (!pack.importable()) {
                    files.add(FrameworkBundleFileResult.skipped(
                            step++, pack.moduleFolder(), pack.relativePath(), pack.filename(),
                            pack.skipReason()));
                    skipped++;
                }
            }
        }

        return new FrameworkBundleImportResultResponse(
                scope.name(),
                moduleFolder,
                relativePath,
                dryRun,
                imported,
                skipped,
                errors,
                files);
    }

    private static String summarize(FrameworkImportResultResponse result) {
        if (result == null || result.report() == null) {
            return "done";
        }
        long skipped = result.report().stream().filter(r -> "SKIPPED".equals(r.status())).count();
        long ok = result.report().size() - skipped;
        return ok + " report line(s), " + skipped + " skipped";
    }
}
