package com.a9.arbor.grc.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Client-side routes that are not separate static files: forward to the SPA shell.
 * Other assets are served from {@code classpath:/static/} by Spring Boot defaults.
 */
@Controller
public class SpaForwardController {

    @GetMapping("/vendor-portal")
    public String vendorPortal() {
        return "forward:/index.html";
    }

    @GetMapping("/external-audit")
    public String externalAuditPortal() {
        return "forward:/index.html";
    }

    @GetMapping("/signin")
    public String signIn() {
        return "forward:/index.html";
    }

    @GetMapping("/signin/reset")
    public String signInReset() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String loginAlias() {
        return "forward:/index.html";
    }

    @GetMapping("/ops/signin")
    public String operatorSignIn() {
        return "forward:/index.html";
    }

    @GetMapping("/modules/{code}")
    public String licensedModule(@PathVariable String code) {
        if (code == null || code.isBlank() || code.contains("/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }

    @GetMapping("/{nav}")
    public String appNav(@PathVariable String nav) {
        if (nav != null && nav.contains(".")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!SpaRoutes.APP_SEGMENTS.contains(nav)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
