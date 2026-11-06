package com.a9.arbor.grc.catalog.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Branded HTML + plain-text licence expiry mail (SMTP/ACS + outbox).
 * Cream page + gold header look kept; copy matches Arbor Email Template.docx.
 */
public final class LicenseExpiryEmailComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private LicenseExpiryEmailComposer() {
    }

    public record Content(String subject, String plain, String html) {
    }

    public static Content compose(
            String tenantName,
            String tenantId,
            String whenLabel,
            LocalDate expiresOn,
            List<String> moduleLabels,
            boolean expired,
            String signInUrl
    ) {
        return compose(tenantName, tenantId, whenLabel, expiresOn, moduleLabels, expired, signInUrl, null);
    }

    public static Content compose(
            String tenantName,
            String tenantId,
            String whenLabel,
            LocalDate expiresOn,
            List<String> moduleLabels,
            boolean expired,
            String signInUrl,
            String recipientName
    ) {
        String orgName = displayWorkspace(tenantName, tenantId);
        String modules = joinAnd(moduleLabels);
        String date = expiresOn == null ? "-" : DATE.format(expiresOn);
        String status = expired ? "Expired" : "Active";
        String greeting = greeting(recipientName);

        String subject;
        String noticeLine;
        String headline;
        String lead;
        String action;
        if (expired) {
            subject = "Action Required | Your Arbor GRC Subscription Has Expired";
            noticeLine = "LICENSE EXPIRY NOTICE";
            headline = "Your Arbor GRC Subscription Has Expired";
            lead = "This is a notice that your Arbor GRC subscription for " + orgName
                    + " expired on " + date + ". To restore full access to Arbor GRC and all subscribed modules, "
                    + "please coordinate with your Arbor account representative to renew your subscription.";
            action = "Please contact your Arbor account representative to discuss your renewal options "
                    + "and restore continuous service availability.";
        } else {
            subject = "Action Recommended | Your Arbor GRC Subscription Expires " + subjectWhen(whenLabel);
            noticeLine = "LICENSE EXPIRY NOTICE";
            headline = "Your Arbor GRC Subscription Expires on " + date;
            lead = "This is a friendly reminder that your Arbor GRC subscription for " + orgName
                    + " is scheduled to expire on " + date + ". To ensure uninterrupted access to Arbor GRC "
                    + "and all subscribed modules, please coordinate with your Arbor account representative "
                    + "to renew your subscription before the expiry date.";
            action = "Please contact your Arbor account representative to discuss your renewal options "
                    + "and ensure continuous service availability.";
        }

        StringBuilder plain = new StringBuilder();
        plain.append(noticeLine).append("\n");
        plain.append(headline).append("\n\n");
        plain.append(greeting).append("\n\n");
        plain.append(lead).append("\n\n");
        plain.append("Organisation: ").append(orgName).append("\n");
        plain.append("Workspace ID: ").append(blank(tenantId)).append("\n");
        plain.append("Licensed Modules: ").append(modules).append("\n");
        plain.append("Expiry Date: ").append(date).append("\n");
        plain.append("Current Status: ").append(status).append("\n\n");
        plain.append(action).append("\n");
        if (signInUrl != null && !signInUrl.isBlank()) {
            plain.append("\nSign in to Arbor GRC: ").append(signInUrl).append("\n");
        }
        plain.append("\nArbor GRC\nThis notice was sent to Tenant Admin and Owner accounts.\n");

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#F4F1EA;font-family:'Segoe UI',Calibri,Arial,sans-serif;color:#0A1F33;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F1EA;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellpadding="0" cellspacing="0" style="max-width:640px;width:100%%;background:#FFFFFF;border:1px solid #E4D9C5;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="background:#C4A35A;padding:18px 28px;">
                              <p style="margin:0;font-size:13px;letter-spacing:0.14em;text-transform:uppercase;color:#FFFFFF;font-weight:700;">Arbor GRC</p>
                              <p style="margin:8px 0 0;font-size:12px;letter-spacing:0.1em;text-transform:uppercase;color:#FFFFFF;font-weight:700;opacity:0.92;">%s</p>
                              <p style="margin:6px 0 0;font-size:20px;font-weight:700;color:#FFFFFF;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <p style="margin:0 0 14px;font-size:15px;line-height:1.55;color:#0A1F33;">%s</p>
                              <p style="margin:0 0 18px;font-size:15px;line-height:1.55;color:#0A1F33;">%s</p>
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#FBF8F1;border:1px solid #E4D9C5;border-radius:6px;">
                                <tr>
                                  <td style="padding:16px 18px;">
                                    %s
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:18px 0 0;font-size:15px;line-height:1.55;color:#0A1F33;">%s</p>
                              %s
                              <p style="margin:22px 0 0;font-size:12px;line-height:1.5;color:#5C6B7A;">This notice was sent to Tenant Admin and Owner accounts. Reminders are sent 3 months, 2 months, 1 month, 2 weeks, 1 week, and 1 day before expiry.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#0A1F33;padding:14px 28px;">
                              <p style="margin:0;font-size:12px;color:#E4D9C5;">Arbor GRC · Commercial licence notice</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(subject),
                escape(noticeLine),
                escape(headline),
                escape(greeting),
                escape(lead),
                detailRows(orgName, tenantId, modules, date, status),
                escape(action),
                ctaHtml(signInUrl)
        );
        return new Content(subject, plain.toString(), html);
    }

    private static String detailRows(String orgName, String tenantId, String modules, String date, String status) {
        return row("Organisation", orgName)
                + row("Workspace ID", blank(tenantId))
                + row("Licensed Modules", modules)
                + row("Expiry Date", date)
                + row("Current Status", status);
    }

    private static String row(String label, String value) {
        return "<p style=\"margin:0 0 8px;font-size:14px;color:#0A1F33;\"><strong style=\"display:inline-block;min-width:140px;color:#8A6A2F;\">"
                + escape(label) + "</strong> " + escape(value) + "</p>";
    }

    private static String ctaHtml(String signInUrl) {
        if (signInUrl == null || signInUrl.isBlank()) {
            return "";
        }
        return "<p style=\"margin:22px 0 0;\"><a href=\"" + escape(signInUrl)
                + "\" style=\"display:inline-block;background:#C4A35A;color:#FFFFFF;text-decoration:none;"
                + "font-size:14px;font-weight:700;padding:10px 18px;border-radius:4px;\">Sign in to Arbor GRC</a></p>";
    }

    /** "in 2 months" → "in 2 Months"; "tomorrow" → "tomorrow". */
    static String subjectWhen(String whenLabel) {
        if (whenLabel == null || whenLabel.isBlank()) {
            return "soon";
        }
        String[] parts = whenLabel.trim().split("\\s+");
        if (parts.length == 0) {
            return whenLabel.trim();
        }
        String last = parts[parts.length - 1];
        parts[parts.length - 1] = Character.toUpperCase(last.charAt(0)) + last.substring(1);
        return String.join(" ", parts);
    }

    private static String greeting(String recipientName) {
        if (recipientName == null || recipientName.isBlank()
                || recipientName.trim().equalsIgnoreCase("Tenant Owner")) {
            return "Dear Sir or Madam,";
        }
        return "Dear " + recipientName.trim() + ",";
    }

    private static String displayWorkspace(String tenantName, String tenantId) {
        if (tenantName != null && !tenantName.isBlank()) {
            return tenantName.trim();
        }
        return tenantId == null || tenantId.isBlank() ? "your organisation" : tenantId;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String joinAnd(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Licensed modules";
        }
        if (labels.size() == 1) {
            return labels.get(0);
        }
        if (labels.size() == 2) {
            return labels.get(0) + " and " + labels.get(1);
        }
        return String.join(", ", labels.subList(0, labels.size() - 1)) + ", and " + labels.get(labels.size() - 1);
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
