package com.a9.arbor.grc.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Browser session cookies. The SPA cannot read these. API clients keep using Authorization.
 * {@value #PERSIST} is not a secret: it only says the refresh cookie should survive a browser restart.
 */
@Component
public class SessionCookies {

    public static final String ACCESS = "arbor_access";
    public static final String REFRESH = "arbor_refresh";
    public static final String PERSIST = "arbor_persist";
    public static final String HEADER = "X-Arbor-Session";
    static final String REFRESH_PATH = "/api/auth";

    public static boolean requested(HttpServletRequest request) {
        return request != null && "cookie".equalsIgnoreCase(request.getHeader(HEADER));
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      String accessToken, String refreshToken, long accessMinutes, boolean persistent) {
        if (accessToken != null && !accessToken.isBlank()) {
            add(request, response, ACCESS, accessToken, "/", maxAge(persistent, accessMinutes * 60));
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            long refreshSeconds = com.a9.arbor.grc.modules.iam.service.RefreshTokenService.REFRESH_DAYS * 24 * 3600;
            add(request, response, REFRESH, refreshToken, REFRESH_PATH, maxAge(persistent, refreshSeconds));
        }
        if (persistent) {
            long refreshSeconds = com.a9.arbor.grc.modules.iam.service.RefreshTokenService.REFRESH_DAYS * 24 * 3600;
            add(request, response, PERSIST, "1", "/", refreshSeconds);
        } else {
            clearOne(request, response, PERSIST, "/");
        }
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        clearOne(request, response, ACCESS, "/");
        clearOne(request, response, REFRESH, REFRESH_PATH);
        clearOne(request, response, PERSIST, "/");
    }

    /** Drops only the access cookie. The refresh cookie may still be valid and is checked by /api/auth/refresh. */
    public static void clearAccess(HttpServletRequest request, HttpServletResponse response) {
        clearOne(request, response, ACCESS, "/");
    }

    public static String accessToken(HttpServletRequest request) {
        return cookie(request, ACCESS);
    }

    public static String refreshToken(HttpServletRequest request) {
        return cookie(request, REFRESH);
    }

    public static boolean persistent(HttpServletRequest request) {
        return "1".equals(cookie(request, PERSIST));
    }

    private static long maxAge(boolean persistent, long seconds) {
        return persistent ? seconds : -1;
    }

    private static void add(HttpServletRequest request, HttpServletResponse response,
                            String name, String value, String path, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(!PERSIST.equals(name))
                .secure(secure(request))
                .path(path)
                .sameSite("Lax");
        if (maxAgeSeconds >= 0) {
            builder.maxAge(maxAgeSeconds);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private static void clearOne(HttpServletRequest request, HttpServletResponse response,
                                 String name, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(!PERSIST.equals(name))
                .secure(secure(request))
                .path(path)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    static boolean secure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwarded = request.getHeader("X-Forwarded-Proto");
        return forwarded != null && forwarded.toLowerCase().startsWith("https");
    }

    private static String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }
}
