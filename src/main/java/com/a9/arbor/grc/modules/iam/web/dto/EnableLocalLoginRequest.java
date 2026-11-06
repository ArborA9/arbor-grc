package com.a9.arbor.grc.modules.iam.web.dto;

/**
 * Password is required only when mail is not live. When mail is live the server emails a set-password link.
 */
public record EnableLocalLoginRequest(String password) {
}
