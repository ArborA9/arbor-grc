package com.a9.arbor.grc.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * SPA shell for JAR deploys (local smoke + Azure). Vite dev server is unaffected.
 * Favicons are mapped here because {@code GET /{nav}} would otherwise 404 {@code /favicon.ico}.
 * Older Chrome always fetches {@code /favicon.ico} and does not fall back to PNG {@code <link>}
 * tags. That path is PNG bytes (same 32x32 as {@code favicon-32.png}), not an ICO container.
 */
@Controller
public class SpaShellController {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> index() {
        if (!INDEX.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noCache())
                .body(INDEX);
    }

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Resource> faviconIco() {
        return classpathFile("static/favicon.ico", MediaType.IMAGE_PNG);
    }

    @GetMapping("/favicon.svg")
    @ResponseBody
    public ResponseEntity<Resource> faviconSvg() {
        return classpathFile("static/favicon.svg", MediaType.parseMediaType("image/svg+xml"));
    }

    @GetMapping("/favicon-32.png")
    @ResponseBody
    public ResponseEntity<Resource> faviconPng32() {
        return classpathFile("static/favicon-32.png", MediaType.IMAGE_PNG);
    }

    @GetMapping("/favicon-16.png")
    @ResponseBody
    public ResponseEntity<Resource> faviconPng16() {
        return classpathFile("static/favicon-16.png", MediaType.IMAGE_PNG);
    }

    @GetMapping("/apple-touch-icon.png")
    @ResponseBody
    public ResponseEntity<Resource> appleTouchIcon() {
        return classpathFile("static/apple-touch-icon.png", MediaType.IMAGE_PNG);
    }

    private static ResponseEntity<Resource> classpathFile(String path, MediaType type) {
        Resource file = new ClassPathResource(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.noCache())
                .body(file);
    }
}
