package com.a9.arbor.grc.modules.license.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.service.LicenseExpiryEmailComposer;
import com.a9.arbor.grc.catalog.service.LicenseExpiryNoticeService;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sends licence expiry in-app + HTML email to Tenant Admin and Owner in the current tenant DB. */
@Service
public class LicenseExpiryMailService {

    private static final Logger log = LoggerFactory.getLogger(LicenseExpiryMailService.class);

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ArborNotificationProperties notificationProperties;

    public LicenseExpiryMailService(UserRepository userRepository,
                                    NotificationService notificationService,
                                    ArborNotificationProperties notificationProperties) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationProperties = notificationProperties;
    }

    @Transactional
    public int send(Tenant tenant, LicenseExpiryNoticeService.DueBatch batch) {
        List<User> recipients = tenantAdmins();
        if (recipients.isEmpty()) {
            log.warn("Licence expiry notice skipped for {}: no active Owner/Admin", tenant.getTenantId());
            return 0;
        }
        List<String> labels = new ArrayList<>();
        for (LicenseExpiryNoticeService.DueModule module : batch.modules()) {
            labels.add(module.code().name());
        }
        String signIn = signInUrl();
        String trigger = batch.milestone().isExpired() ? "LICENSE_EXPIRED" : "LICENSE_EXPIRY_REMINDER";
        String moduleList = String.join(", ", labels);
        Map<String, String> data = Map.of(
                "whenLabel", batch.milestone().whenLabel(),
                "expiresOn", batch.earliestExpiry() == null ? "-" : batch.earliestExpiry().toString(),
                "moduleList", moduleList,
                "workspaceId", tenant.getTenantId());
        notificationService.trigger(trigger, tenant.getTenantId() + ":" + batch.milestone().name(),
                List.of("OWNER", "ADMIN"), data);
        int sent = 0;
        for (User user : recipients) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            LicenseExpiryEmailComposer.Content mail = LicenseExpiryEmailComposer.compose(
                    tenant.getName(),
                    tenant.getTenantId(),
                    batch.milestone().whenLabel(),
                    batch.earliestExpiry(),
                    labels,
                    batch.milestone().isExpired(),
                    signIn,
                    user.getFullName());
            notificationService.queueHtmlEmail(trigger, user.getEmail(), mail.subject(), mail.plain(), mail.html());
            sent++;
        }
        return sent;
    }

    private List<User> tenantAdmins() {
        Set<User> users = new LinkedHashSet<>();
        users.addAll(userRepository.findByRolesContainingAndStatus(Role.OWNER, UserStatus.ACTIVE));
        users.addAll(userRepository.findByRolesContainingAndStatus(Role.ADMIN, UserStatus.ACTIVE));
        return new ArrayList<>(users);
    }

    private String signInUrl() {
        String base = notificationProperties.getAppBaseUrl();
        if (base == null || base.isBlank()) {
            return "";
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return trimmed + "/signin";
    }
}
