package com.a9.arbor.grc.modules.notification.delivery;

import com.a9.arbor.grc.modules.notification.domain.NotificationEmailOutbox;
import com.a9.arbor.grc.modules.notification.repo.NotificationEmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends one already-queued outbox row. Runs on the email worker thread, after the
 * request transaction has committed, with {@code TenantContext} already set.
 */
@Service
public class EmailOutboxSendWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxSendWorker.class);

    private final NotificationEmailOutboxRepository outboxRepository;
    private final EmailDeliveryService emailDeliveryService;

    public EmailOutboxSendWorker(NotificationEmailOutboxRepository outboxRepository,
                                 EmailDeliveryService emailDeliveryService) {
        this.outboxRepository = outboxRepository;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Transactional
    public void deliverQueued(long outboxId) {
        NotificationEmailOutbox row = outboxRepository.findById(outboxId).orElse(null);
        if (row == null || !EmailDeliveryOutcome.STATUS_PENDING.equals(row.getStatus())) {
            return;
        }
        emailDeliveryService.deliver(row);
        outboxRepository.save(row);
        log.info("Email outbox delivered: trigger={} to={} status={}",
                row.getTriggerType(), row.getToEmail(), row.getStatus());
    }
}
