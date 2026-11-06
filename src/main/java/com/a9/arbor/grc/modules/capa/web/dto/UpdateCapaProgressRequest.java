package com.a9.arbor.grc.modules.capa.web.dto;

public record UpdateCapaProgressRequest(
        String rootCause,
        String correctiveAction,
        String preventiveAction,
        String progressNotes
) {
}
