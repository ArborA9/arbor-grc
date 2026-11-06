package com.a9.arbor.grc.modules.notification.service;

import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.domain.Notification;
import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;
import com.a9.arbor.grc.modules.notification.domain.NotificationTemplate;
import com.a9.arbor.grc.modules.notification.delivery.EmailDeliveryService;
import com.a9.arbor.grc.modules.notification.delivery.EmailOutboxDispatcher;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.modules.notification.mail.AppNotificationMailComposer;
import com.a9.arbor.grc.modules.notification.mail.NotificationMailContent;
import com.a9.arbor.grc.modules.notification.repo.NotificationEmailOutboxRepository;
import com.a9.arbor.grc.modules.notification.repo.NotificationRepository;
import com.a9.arbor.grc.modules.notification.repo.NotificationTemplateRepository;
import com.a9.arbor.grc.modules.notification.web.dto.NotificationEmailOutboxResponse;
import com.a9.arbor.grc.modules.notification.web.dto.NotificationResponse;
import com.a9.arbor.grc.modules.notification.web.dto.NotificationTemplateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central notification engine (req §7). Modules must call {@link #trigger}: never send mail/in-app themselves.
 * Email delivery: see {@link ArborNotificationProperties} and {@link EmailDeliveryService}.
 * Local default is OUTBOX (queue only). Live SMTP/ACS is queued on the request and sent
 * after commit, so the caller does not wait for the mail provider.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationEmailOutboxRepository outboxRepository;
    private final UserRepository userRepository;
    private final AuditTrailService auditTrailService;
    private final ArborNotificationProperties notificationProperties;
    private final EmailDeliveryService emailDeliveryService;
    private final EmailOutboxDispatcher emailOutboxDispatcher;
    private final AppNotificationMailComposer appMailComposer;

    public NotificationService(NotificationTemplateRepository templateRepository,
                               NotificationRepository notificationRepository,
                               NotificationEmailOutboxRepository outboxRepository,
                               UserRepository userRepository,
                               AuditTrailService auditTrailService,
                               ArborNotificationProperties notificationProperties,
                               EmailDeliveryService emailDeliveryService,
                               EmailOutboxDispatcher emailOutboxDispatcher,
                               AppNotificationMailComposer appMailComposer) {
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
        this.auditTrailService = auditTrailService;
        this.notificationProperties = notificationProperties;
        this.emailDeliveryService = emailDeliveryService;
        this.emailOutboxDispatcher = emailOutboxDispatcher;
        this.appMailComposer = appMailComposer;
    }

    /**
     * @param triggerType e.g. USER_INVITED
     * @param recordId    related record id (string)
     * @param recipients  user ids, email addresses, and/or role names (OWNER, ADMIN, …)
     * @param data        template variables ({{key}})
     */
    @Transactional
    public void trigger(String triggerType, String recordId, List<String> recipients, Map<String, String> data) {
        try {
            doTrigger(triggerType, recordId, recipients, data != null ? data : Map.of(), true, true);
        } catch (Exception e) {
            log.error("Notification trigger {} failed: {}", triggerType, e.getMessage(), e);
        }
    }

    /** Bell only. Use when email is sent once for the whole action. */
    public void triggerInApp(String triggerType, String recordId, List<String> recipients, Map<String, String> data) {
        try {
            doTrigger(triggerType, recordId, recipients, data != null ? data : Map.of(), true, false);
        } catch (Exception e) {
            log.error("Notification trigger {} failed: {}", triggerType, e.getMessage(), e);
        }
    }

    /** Email only. Use for one message that lists every control in the action. */
    public void triggerEmail(String triggerType, String recordId, List<String> recipients, Map<String, String> data) {
        try {
            doTrigger(triggerType, recordId, recipients, data != null ? data : Map.of(), false, true);
        } catch (Exception e) {
            log.error("Notification trigger {} failed: {}", triggerType, e.getMessage(), e);
        }
    }

    private void doTrigger(String triggerType, String recordId, List<String> recipients, Map<String, String> data,
                           boolean allowInApp, boolean allowEmail) {
        Optional<NotificationTemplate> templateOpt =
                templateRepository.findFirstByTriggerTypeAndActiveTrueOrderByVersionDesc(triggerType);
        if (templateOpt.isEmpty()) {
            log.warn("No active template for trigger {}", triggerType);
            return;
        }
        NotificationTemplate template = templateOpt.get();
        Map<String, String> vars = NotificationCopy.enrich(data);
        String title = render(template.getSubjectTemplate(), vars);
        String body = render(template.getBodyTemplate(), vars);

        List<User> users = resolveRecipients(recipients);
        if (users.isEmpty()) {
            log.warn("No recipients resolved for trigger {} recipients={}", triggerType, recipients);
            return;
        }

        for (User user : users) {
            if (user.getStatus() == UserStatus.DISABLED) {
                continue;
            }
            if (allowInApp && template.isChannelInApp() && user.getRoles().stream().noneMatch(r -> r == Role.VENDOR)) {
                Notification n = new Notification();
                n.setUserId(user.getId());
                n.setTriggerType(triggerType);
                n.setTitle(trim(title, 500));
                n.setBody(trim(body, 2000));
                n.setRecordType("Notification");
                n.setRecordId(recordId);
                n.setLink(NotificationLinkResolver.navIdFor(triggerType));
                notificationRepository.save(n);
            }
            if (allowEmail && template.isChannelEmail() && notificationProperties.queueEmail()) {
                Map<String, String> mailVars = new java.util.HashMap<>(vars);
                mailVars.put(AppNotificationMailComposer.RECIPIENT_ID, String.valueOf(user.getId()));
                queueBranded(triggerType, user.getEmail(), title, body, mailVars);
            } else if (allowEmail && template.isChannelEmail()) {
                log.debug("Email channel skipped (email-delivery=DISABLED) for trigger={}", triggerType);
            }
        }

        auditTrailService.record(AuditActionType.CREATED, "NOTIFICATION", null, "SYSTEM",
                "Notification", recordId, triggerType, null, null,
                "recipients=" + users.size(), null, null);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> latestForUser(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationRepository.markRead(notificationId, userId, Instant.now());
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<NotificationEmailOutboxResponse> listEmailOutbox(Pageable pageable) {
        return outboxRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(row -> NotificationEmailOutboxResponse.from(row, emailDeliveryService));
    }

    @Transactional(readOnly = true)
    public String emailDeliveryMode() {
        return notificationProperties.getEmailDelivery();
    }

    /** Path C B5: latest version per trigger (for Admin enable/disable). */
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> listTemplatesLatest() {
        Map<String, NotificationTemplate> latest = new java.util.LinkedHashMap<>();
        for (NotificationTemplate t : templateRepository.findAllByOrderByTriggerTypeAscVersionDesc()) {
            latest.putIfAbsent(t.getTriggerType(), t);
        }
        return latest.values().stream()
                .map(t -> new NotificationTemplateResponse(
                        t.getId(), t.getTriggerType(), t.getSubjectTemplate(),
                        t.isChannelEmail(), t.isChannelInApp(), t.getVersion(), t.isActive()))
                .toList();
    }

    @Transactional
    public NotificationTemplateResponse setTemplateActive(long id, boolean active) {
        NotificationTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        t.setActive(active);
        templateRepository.save(t);
        return new NotificationTemplateResponse(
                t.getId(), t.getTriggerType(), t.getSubjectTemplate(),
                t.isChannelEmail(), t.isChannelInApp(), t.getVersion(), t.isActive());
    }

    /**
     * Queue email to an arbitrary address (vendor / external-auditor OTP flows).
     * Uses active notification templates when present; falls back to rendered defaults.
     */
    @Transactional
    public void queueDirectEmail(String toEmail, String triggerType, Map<String, String> data) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        if (!notificationProperties.queueEmail()) {
            log.debug("Email channel skipped (email-delivery=DISABLED) for direct trigger={}", triggerType);
            return;
        }
        Optional<NotificationTemplate> templateOpt =
                templateRepository.findFirstByTriggerTypeAndActiveTrueOrderByVersionDesc(triggerType);
        Map<String, String> vars = NotificationCopy.enrich(data);
        String subject;
        String body;
        if (templateOpt.isPresent()) {
            NotificationTemplate template = templateOpt.get();
            subject = render(template.getSubjectTemplate(), vars);
            body = render(template.getBodyTemplate(), vars);
        } else {
            subject = triggerType;
            body = vars.toString();
        }
        queueBranded(triggerType, toEmail.trim(), subject, body, vars);
    }

    private void queueBranded(String triggerType, String toEmail, String subject, String body, Map<String, String> vars) {
        NotificationMailContent mail = appMailComposer.compose(triggerType, subject, body, vars);
        queueEmail(triggerType, toEmail, trim(mail.subject(), 500), trim(mail.plain(), 8000), mail.html());
    }

    /**
     * Queue a branded HTML email (licence notices). Plain body is kept for outbox inspection and
     * clients that cannot render HTML.
     */
    @Transactional
    public void queueHtmlEmail(String triggerType, String toEmail, String subject, String plainBody, String htmlBody) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        if (!notificationProperties.queueEmail()) {
            log.debug("Email channel skipped (email-delivery=DISABLED) for html trigger={}", triggerType);
            return;
        }
        queueEmail(triggerType, toEmail.trim(), trim(subject, 500), trim(plainBody, 8000), htmlBody);
    }

    private void queueEmail(String triggerType, String toEmail, String subject, String body) {
        queueEmail(triggerType, toEmail, subject, body, null);
    }

    private void queueEmail(String triggerType, String toEmail, String subject, String body, String htmlBody) {
        NotificationEmailOutbox mail = new NotificationEmailOutbox();
        mail.setTriggerType(triggerType);
        mail.setToEmail(toEmail);
        mail.setSubject(subject);
        mail.setBody(body);
        mail.setBodyHtml(htmlBody);
        mail.setStatus("PENDING");
        if (notificationProperties.emailLive()) {
            NotificationEmailOutbox saved = outboxRepository.save(mail);
            log.info("Email queued for delivery: trigger={} to={} mode={}",
                    triggerType, toEmail, notificationProperties.getEmailDelivery());
            if (saved.getId() != null) {
                emailOutboxDispatcher.scheduleAfterCommit(TenantContext.getTenantId(), saved.getId());
            }
            return;
        }
        emailDeliveryService.deliver(mail);
        outboxRepository.save(mail);
        log.info("Email outbox: trigger={} to={} mode={} status={}",
                triggerType, toEmail, notificationProperties.getEmailDelivery(), mail.getStatus());
    }

    private List<User> resolveRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        Set<User> resolved = new LinkedHashSet<>();
        for (String raw : recipients) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String token = raw.trim();
            try {
                Long id = Long.parseLong(token);
                userRepository.findById(id).ifPresent(resolved::add);
                continue;
            } catch (NumberFormatException ignored) {
                /* role name or email */
            }
            if (token.contains("@")) {
                userRepository.findByEmailIgnoreCase(token).ifPresentOrElse(
                        resolved::add,
                        () -> log.warn("No user for notification email {}", token));
                continue;
            }
            try {
                Role role = Role.valueOf(token.toUpperCase(Locale.ROOT));
                resolved.addAll(userRepository.findByRolesContainingAndStatus(role, UserStatus.ACTIVE));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown recipient token: {}", token);
            }
        }
        return new ArrayList<>(resolved);
    }

    private static String render(String template, Map<String, String> data) {
        String out = template;
        for (Map.Entry<String, String> e : data.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue() : "");
        }
        return out;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
