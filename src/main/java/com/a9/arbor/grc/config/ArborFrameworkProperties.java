package com.a9.arbor.grc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Framework pack folder for Super Admin import scopes (one / module / all).
 * Packs are read from the configured directory when it exists. The Azure JAR uses
 * packs copied to {@code classpath:framework-packs/} at Maven build.
 */
@ConfigurationProperties(prefix = "arbor.frameworks")
public class ArborFrameworkProperties {

    /**
     * Root directory containing module folders with framework {@code .xlsx} files.
     * Override with {@code ARBOR_FRAMEWORKS_DIR}.
     */
    private String dir = "docs/frameworks";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
