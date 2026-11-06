package com.a9.arbor.grc.security.oidc;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Sends SSO handshake failures back to the SPA login screen with an error marker. */
@Component
public class OidcLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcLoginFailureHandler.class);

    private final String frontendBaseUrl;

    public OidcLoginFailureHandler(ArborSecurityProperties properties) {
        this.frontendBaseUrl = properties.getSso().getFrontendBaseUrl();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("SSO handshake failed: {}", exception.getMessage());
        response.sendRedirect(frontendBaseUrl + "/signin#sso_error=sso_failed");
    }
}
