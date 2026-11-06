package com.a9.arbor.grc.modules.export;

/** Binary download payload for assessment exports. */
public record ExportFile(byte[] bytes, String filename, String contentType) {
}
