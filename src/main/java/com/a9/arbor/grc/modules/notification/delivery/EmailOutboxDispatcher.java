package com.a9.arbor.grc.modules.notification.delivery;

import com.a9.arbor.grc.tenancy.TenantContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands live SMTP/ACS sends to a small pool after the request commits, so create,
 * submit, and close return without waiting for Azure.
 */
@Service
public class EmailOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxDispatcher.class);

    private final EmailOutboxSendWorker worker;
    private final ExecutorService executor;

    public EmailOutboxDispatcher(EmailOutboxSendWorker worker) {
        this.worker = worker;
        AtomicInteger seq = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "arbor-email-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public void scheduleAfterCommit(String tenantId, long outboxId) {
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Email {} not dispatched: no tenant on the request", outboxId);
            return;
        }
        Runnable job = () -> send(tenantId, outboxId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(job);
                }
            });
        } else {
            executor.execute(job);
        }
    }

    private void send(String tenantId, long outboxId) {
        try {
            TenantContext.setTenantId(tenantId);
            worker.deliverQueued(outboxId);
        } catch (Exception e) {
            log.error("Email dispatch failed tenant={} outboxId={}: {}", tenantId, outboxId, e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
