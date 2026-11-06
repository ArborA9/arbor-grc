package com.a9.arbor.grc.modules.notification.delivery;

import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.config.ArborNotificationProperties.EmailDeliveryMode;
import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;
import com.a9.arbor.grc.modules.notification.service.NotificationCopy;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.SyncPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * Pluggable email delivery for the notification outbox (Letter E).
 * Default {@link EmailDeliveryMode#OUTBOX} preserves existing local/E2E behaviour.
 */
@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final ArborNotificationProperties properties;
    private final Optional<JavaMailSender> mailSender;
    private volatile EmailClient acsClient;

    public EmailDeliveryService(ArborNotificationProperties properties,
                                Optional<JavaMailSender> mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    /**
     * Attempt delivery for a persisted outbox row and apply status fields on the entity.
     */
    public void deliver(NotificationEmailOutbox row) {
        EmailDeliveryOutcome outcome = attemptDelivery(row);
        row.setStatus(outcome.status());
        row.setErrorMessage(outcome.errorMessage());
        if (EmailDeliveryOutcome.STATUS_SENT.equals(outcome.status())) {
            row.setSentAt(Instant.now());
            SecretLinkRedactor.scrubAfterSend(row);
        }
    }

    public String deliveryNote(NotificationEmailOutbox row) {
        return switch (row.getStatus()) {
            case EmailDeliveryOutcome.STATUS_SENT -> "Delivered via " + properties.deliveryMode();
            case EmailDeliveryOutcome.STATUS_FAILED -> row.getErrorMessage();
            case EmailDeliveryOutcome.STATUS_PENDING -> pendingNote();
            default -> null;
        };
    }

    private String pendingNote() {
        return switch (properties.deliveryMode()) {
            case OUTBOX -> "Queued in outbox: not delivered (OUTBOX mode)";
            case ACS -> properties.acsConfigured()
                    ? "Queued: delivery pending"
                    : "Queued: ACS not configured (set connection string + sender)";
            case SMTP -> properties.smtpConfigured()
                    ? "Queued: delivery pending"
                    : "Queued: SMTP host not configured";
            case DISABLED -> "Email channel disabled";
        };
    }

    private EmailDeliveryOutcome attemptDelivery(NotificationEmailOutbox row) {
        return switch (properties.deliveryMode()) {
            case OUTBOX -> EmailDeliveryOutcome.pending();
            case DISABLED -> EmailDeliveryOutcome.pending();
            case SMTP -> sendSmtp(row);
            case ACS -> sendAcs(row);
        };
    }

    private String bodyForSend(NotificationEmailOutbox row) {
        String body = row.getBody() == null ? "" : row.getBody();
        if (NotificationCopy.skipGenericAppLink(row.getTriggerType(), body)) {
            return body;
        }
        String base = properties.getAppBaseUrl();
        if (base == null || base.isBlank()) {
            return body;
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return body + "\n\nOpen Arbor GRC: " + trimmed + "/signin";
    }

    private EmailDeliveryOutcome sendSmtp(NotificationEmailOutbox row) {
        if (!properties.smtpConfigured()) {
            log.warn("SMTP delivery requested but arbor.notifications.smtp.host is not set: keeping PENDING");
            return EmailDeliveryOutcome.pending();
        }
        if (mailSender.isEmpty()) {
            log.warn("SMTP delivery requested but JavaMailSender bean is unavailable: keeping PENDING");
            return EmailDeliveryOutcome.pending();
        }
        try {
            String html = row.getBodyHtml();
            if (html != null && !html.isBlank()) {
                MimeMessage mime = mailSender.get().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
                helper.setFrom(properties.getSmtp().getFrom());
                helper.setTo(row.getToEmail());
                helper.setSubject(row.getSubject());
                helper.setText(bodyForSend(row), html);
                mailSender.get().send(mime);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(properties.getSmtp().getFrom());
                message.setTo(row.getToEmail());
                message.setSubject(row.getSubject());
                message.setText(bodyForSend(row));
                mailSender.get().send(message);
            }
            log.info("Email sent via SMTP: trigger={} to={}", row.getTriggerType(), row.getToEmail());
            return EmailDeliveryOutcome.sent();
        } catch (Exception e) {
            log.error("SMTP send failed for trigger={} to={}: {}", row.getTriggerType(), row.getToEmail(),
                    e.getMessage());
            return EmailDeliveryOutcome.failed(e.getMessage());
        }
    }

    private EmailDeliveryOutcome sendAcs(NotificationEmailOutbox row) {
        if (!properties.acsConfigured()) {
            log.warn("ACS delivery requested but connection string or sender is not set: keeping PENDING");
            return EmailDeliveryOutcome.pending();
        }
        try {
            EmailMessage message = new EmailMessage()
                    .setSenderAddress(properties.getAcs().getSenderAddress())
                    .setToRecipients(new EmailAddress(row.getToEmail()))
                    .setSubject(row.getSubject())
                    .setBodyPlainText(bodyForSend(row));
            if (row.getBodyHtml() != null && !row.getBodyHtml().isBlank()) {
                message.setBodyHtml(row.getBodyHtml());
            }
            SyncPoller<EmailSendResult, EmailSendResult> poller = acsClient().beginSend(message, null);
            EmailSendResult result = poller.getFinalResult();
            if (result.getStatus() == com.azure.communication.email.models.EmailSendStatus.SUCCEEDED) {
                log.info("Email sent via ACS: trigger={} to={} id={}",
                        row.getTriggerType(), row.getToEmail(), result.getId());
                return EmailDeliveryOutcome.sent();
            }
            return EmailDeliveryOutcome.failed("ACS status: " + result.getStatus());
        } catch (Exception e) {
            log.error("ACS send failed for trigger={} to={}: {}", row.getTriggerType(), row.getToEmail(),
                    e.getMessage());
            return EmailDeliveryOutcome.failed(e.getMessage());
        }
    }

    private EmailClient acsClient() {
        EmailClient existing = acsClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (acsClient == null) {
                acsClient = new EmailClientBuilder()
                        .connectionString(properties.getAcs().getConnectionString())
                        .buildClient();
            }
            return acsClient;
        }
    }
}
