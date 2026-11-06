package com.a9.arbor.grc.security.sso;

import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.security.SessionCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public: redeems a one-time SSO code for a JWT (see {@link SsoExchangeService}). */
@RestController
@RequestMapping("/api/auth/sso")
public class SsoExchangeController {

    private final SsoExchangeService exchangeService;
    private final SessionCookies sessionCookies;

    public SsoExchangeController(SsoExchangeService exchangeService, SessionCookies sessionCookies) {
        this.exchangeService = exchangeService;
        this.sessionCookies = sessionCookies;
    }

    @PostMapping("/exchange")
    public LoginResponse exchange(@RequestBody ExchangeRequest request,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        LoginResponse body = exchangeService.redeem(request.code());
        if (!SessionCookies.requested(httpRequest)) {
            return body;
        }
        // SSO has no refresh token. The access cookie lasts for this browser session.
        sessionCookies.write(httpRequest, httpResponse, body.token(), null, body.expiresInMinutes(), false);
        return body.withoutSecrets();
    }

    public record ExchangeRequest(@NotBlank String code) {
    }
}
