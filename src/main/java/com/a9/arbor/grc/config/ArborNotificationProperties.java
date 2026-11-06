package com.a9.arbor.grc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email delivery mode for the notification engine.
 * <ul>
 *   <li>{@code OUTBOX} (default/local): queue {@code notification_email_outbox} rows only; no external send</li>
 *   <li>{@code DISABLED}: in-app only; never queue email</li>
 *   <li>{@code SMTP}: send via JavaMail (Mailhog/Mailtrap/UAT relay)</li>
 *   <li>{@code ACS}: send via Azure Communication Services when connection string is configured;
 *       otherwise rows stay {@code PENDING} (safe until Azure is procured)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "arbor.notifications")
public class ArborNotificationProperties {

    /**
     * OUTBOX | DISABLED | SMTP | ACS
     */
    private String emailDelivery = "OUTBOX";

    /**
     * Optional absolute app URL appended to outbound email bodies (SMTP/ACS only).
     */
    private String appBaseUrl = "";

    private final Smtp smtp = new Smtp();
    private final Acs acs = new Acs();

    public String getEmailDelivery() {
        return emailDelivery;
    }

    public void setEmailDelivery(String emailDelivery) {
        this.emailDelivery = emailDelivery;
    }

    public String getAppBaseUrl() {
        return appBaseUrl;
    }

    public void setAppBaseUrl(String appBaseUrl) {
        this.appBaseUrl = appBaseUrl;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public Acs getAcs() {
        return acs;
    }

    public EmailDeliveryMode deliveryMode() {
        if (emailDelivery == null || emailDelivery.isBlank()) {
            return EmailDeliveryMode.OUTBOX;
        }
        try {
            return EmailDeliveryMode.valueOf(emailDelivery.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EmailDeliveryMode.OUTBOX;
        }
    }

    public boolean queueEmail() {
        return deliveryMode() != EmailDeliveryMode.DISABLED;
    }

    public boolean smtpConfigured() {
        return smtp.getHost() != null && !smtp.getHost().isBlank();
    }

    public boolean acsConfigured() {
        return acs.getConnectionString() != null && !acs.getConnectionString().isBlank()
                && acs.getSenderAddress() != null && !acs.getSenderAddress().isBlank();
    }

    /** True only when a real mail path is configured and selected. */
    public boolean emailLive() {
        return switch (deliveryMode()) {
            case SMTP -> smtpConfigured();
            case ACS -> acsConfigured();
            default -> false;
        };
    }

    public enum EmailDeliveryMode {
        OUTBOX,
        DISABLED,
        SMTP,
        ACS
    }

    public static class Smtp {
        private String host = "";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "notifications@arborgrc.local";
        private boolean starttls = true;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public boolean isStarttls() {
            return starttls;
        }

        public void setStarttls(boolean starttls) {
            this.starttls = starttls;
        }
    }

    public static class Acs {
        private String connectionString = "";
        private String senderAddress = "notifications@arborgrc.com";

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getSenderAddress() {
            return senderAddress;
        }

        public void setSenderAddress(String senderAddress) {
            this.senderAddress = senderAddress;
        }
    }
}
