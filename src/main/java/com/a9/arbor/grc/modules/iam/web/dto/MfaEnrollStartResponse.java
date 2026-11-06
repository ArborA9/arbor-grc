package com.a9.arbor.grc.modules.iam.web.dto;

public record MfaEnrollStartResponse(String secret, String otpauthUri) {
}
