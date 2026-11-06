package com.a9.arbor.grc.modules.bcms.web.dto;

/** Selected person from Entra (directoryId) or Arbor users (userId). */
public record BiaPersonRef(
        String source,
        String directoryId,
        Long userId,
        String email,
        String displayName,
        String jobTitle
) {
}
