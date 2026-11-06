package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.service.CrossFrameworkMappingService;
import com.a9.arbor.grc.catalog.web.dto.CrossFrameworkMappingImportResult;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/cross-framework-mappings")
public class AdminCrossFrameworkController {

    private final CrossFrameworkMappingService mappingService;

    public AdminCrossFrameworkController(CrossFrameworkMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping("/sample.xlsx")
    public ResponseEntity<byte[]> sample() {
        byte[] bytes = mappingService.sampleExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionSupport.attachment("cross-framework-mappings-sample.xlsx"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CrossFrameworkMappingImportResult importMappings(@RequestParam("file") MultipartFile file) {
        return mappingService.importExcel(file);
    }
}
