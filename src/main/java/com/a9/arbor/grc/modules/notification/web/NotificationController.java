package com.a9.arbor.grc.modules.notification.web;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.notification.web.dto.NotificationEmailOutboxResponse;
import com.a9.arbor.grc.modules.notification.web.dto.NotificationResponse;
import com.a9.arbor.grc.modules.notification.web.dto.NotificationTemplateResponse;
import com.a9.arbor.grc.modules.notification.web.dto.UpdateNotificationTemplateActiveRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * In-app notification inbox (req §7.2). Vendors have no bell.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public Object list(Authentication authentication,
                       @RequestParam(defaultValue = "false") boolean latest,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "50") int size) {
        User user = currentUser(authentication);
        if (latest) {
            return Map.of("notifications", notificationService.latestForUser(user.getId()),
                    "unreadCount", notificationService.unreadCount(user.getId()));
        }
        int safe = Math.min(Math.max(size, 1), 100);
        Page<NotificationResponse> result =
                notificationService.listForUser(user.getId(), PageRequest.of(page, safe));
        return result;
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public Map<String, Long> unreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        return Map.of("unreadCount", notificationService.unreadCount(user.getId()));
    }

    /**
     * Admin view of queued/sent email rows. Delivery mode: OUTBOX (local default), SMTP, or ACS.
     */
    @GetMapping("/email-outbox")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> emailOutbox(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        int safe = Math.min(Math.max(size, 1), 100);
        Page<NotificationEmailOutboxResponse> result =
                notificationService.listEmailOutbox(PageRequest.of(page, safe));
        String mode = notificationService.emailDeliveryMode();
        String note = switch (mode == null ? "OUTBOX" : mode.trim().toUpperCase()) {
            case "SMTP" -> "SMTP mode: messages send when host is configured";
            case "ACS" -> "ACS mode: messages send when connection string + sender are configured";
            case "DISABLED" -> "Email channel disabled: in-app only";
            default -> "OUTBOX mode: rows queued locally for inspection (default dev/E2E)";
        };
        return Map.of(
                "emailDelivery", mode,
                "note", note,
                "page", result);
    }

    /** Path C B5: list latest template per trigger (Owner/Admin). */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<NotificationTemplateResponse> listTemplates() {
        return notificationService.listTemplatesLatest();
    }

    @PutMapping("/templates/{id}/active")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public NotificationTemplateResponse setTemplateActive(@PathVariable long id,
                                                          @Valid @RequestBody UpdateNotificationTemplateActiveRequest request) {
        return notificationService.setTemplateActive(id, request.active());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public Map<String, String> markRead(Authentication authentication, @PathVariable long id) {
        User user = currentUser(authentication);
        notificationService.markRead(user.getId(), id);
        return Map.of("status", "ok");
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public Map<String, String> markAllRead(Authentication authentication) {
        User user = currentUser(authentication);
        notificationService.markAllRead(user.getId());
        return Map.of("status", "ok");
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().noneMatch(r -> r == Role.VENDOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No notification inbox"));
    }
}
