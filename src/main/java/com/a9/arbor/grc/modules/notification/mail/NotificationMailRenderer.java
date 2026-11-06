package com.a9.arbor.grc.modules.notification.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Arbor A1-A10 table HTML (Option A gold). One primary CTA. Text wordmark. Escapes tokens.
 */
public final class NotificationMailRenderer {

    public enum Urgency {
        ACTION("#B8860B", "#92530A", "#FEF3C7", "#854D0E"),
        RETURNED("#EA580C", "#C2410C", "#FFEDD5", "#9A3412"),
        INFO("#57534E", "#92530A", "#E7E5E4", "#44403C");

        final String bar;
        final String btn;
        final String pillBg;
        final String pillFg;

        Urgency(String bar, String btn, String pillBg, String pillFg) {
            this.bar = bar;
            this.btn = btn;
            this.pillBg = pillBg;
            this.pillFg = pillFg;
        }
    }

    public record Detail(String label, String value) {
    }

    public static final class Spec {
        private String workspace = "Arbor GRC";
        private String preheader = "";
        private String pill = "ACTION";
        private Urgency urgency = Urgency.ACTION;
        private String headline = "";
        private String lede = "";
        private final List<Detail> details = new ArrayList<>();
        private String callout;
        private String otpCode;
        private String otpHint;
        private String ctaLabel;
        private String ctaUrl;
        private String afterCta;
        private String note;
        private String whyYouGotThis = "";
        private boolean showAccountLine = true;
        private String triggerCode = "";
        private String messageId = "ARB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private boolean external;
        private String signOff;
        /** Off by default: Arbor Email Template.docx asked to drop why / Azure / Trigger / Message ID. */
        private boolean showFooter = false;
        private boolean showCtaUrl = true;
        private String ctaUrlLabel;
        private boolean showMeta = false;

        public Spec workspace(String v) { this.workspace = blankTo(v, this.workspace); return this; }
        public Spec preheader(String v) { this.preheader = nullToEmpty(v); return this; }
        public Spec pill(String v) { this.pill = blankTo(v, this.pill); return this; }
        public Spec urgency(Urgency v) { if (v != null) this.urgency = v; return this; }
        public Spec headline(String v) { this.headline = nullToEmpty(v); return this; }
        public Spec lede(String v) { this.lede = nullToEmpty(v); return this; }
        public Spec detail(String label, String value) {
            if (value != null && !value.isBlank()) {
                this.details.add(new Detail(label, value));
            }
            return this;
        }
        public Spec callout(String v) { this.callout = blankToNull(v); return this; }
        public Spec otp(String code, String hint) {
            this.otpCode = blankToNull(code);
            this.otpHint = blankToNull(hint);
            return this;
        }
        public Spec cta(String label, String url) {
            this.ctaLabel = blankToNull(label);
            this.ctaUrl = blankToNull(url);
            return this;
        }
        public Spec afterCta(String v) { this.afterCta = blankToNull(v); return this; }
        /** Body paragraph under the details and above the button. */
        public Spec note(String v) { this.note = blankToNull(v); return this; }
        public Spec whyYouGotThis(String v) { this.whyYouGotThis = nullToEmpty(v); return this; }
        public Spec showAccountLine(boolean v) { this.showAccountLine = v; return this; }
        public Spec triggerCode(String v) { this.triggerCode = nullToEmpty(v); return this; }
        public Spec messageId(String v) { this.messageId = blankTo(v, this.messageId); return this; }
        public Spec external(boolean v) { this.external = v; return this; }
        /** Closing lines under the body, one per line (e.g. "Regards,\nGRC Team"). */
        public Spec signOff(String v) { this.signOff = blankToNull(v); return this; }
        /** True restores the why line and (if showMeta) hosting / trigger / message id. Off by default. */
        public Spec showFooter(boolean v) { this.showFooter = v; return this; }
        /** False keeps the button but drops the plain URL line printed under it in HTML. */
        public Spec showCtaUrl(boolean v) { this.showCtaUrl = v; return this; }
        /** Line printed above the plain URL under the button. */
        public Spec ctaUrlLabel(String v) { this.ctaUrlLabel = blankToNull(v); return this; }
        /** True with showFooter adds workspace, hosting, trigger and message id under the why line. Off by default. */
        public Spec showMeta(boolean v) { this.showMeta = v; return this; }

        public NotificationMailContent render(String subject) {
            return new NotificationMailContent(
                    subject == null ? "" : subject.trim(),
                    preheader,
                    NotificationMailRenderer.plain(this),
                    NotificationMailRenderer.html(this));
        }
    }

    private NotificationMailRenderer() {
    }

    public static Spec spec() {
        return new Spec();
    }

    static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String html(Spec s) {
        String pre = escape(s.preheader);
        String workspace = escape(s.workspace);
        String pill = escape(s.pill);
        String headline = escape(s.headline);
        String lede = escape(s.lede).replace("\n", "<br/><br/>");
        String why = escape(s.whyYouGotThis);
        String trigger = escape(s.triggerCode);
        String mid = escape(s.messageId);
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < s.details.size(); i++) {
            Detail d = s.details.get(i);
            boolean last = i == s.details.size() - 1;
            String border = last ? "none" : "1px solid #EDE8DF";
            details.append("<tr><td class=\"db-brdr db-lbl db-cell\" width=\"38%\" style=\"padding:11px 16px;border-bottom:")
                    .append(border)
                    .append(";font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;line-height:18px;color:#726B66;text-transform:uppercase;letter-spacing:0.5px;font-weight:600;vertical-align:top;\">")
                    .append(escape(d.label()))
                    .append("</td><td class=\"db-brdr db-ink db-cell\" style=\"padding:11px 16px;border-bottom:")
                    .append(border)
                    .append(";font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:14px;line-height:20px;color:#1C1917;font-weight:500;vertical-align:top;\">")
                    .append(escape(d.value()))
                    .append("</td></tr>");
        }
        String detailsTable = s.details.isEmpty() ? ""
                : "<div style=\"line-height:20px;font-size:20px;height:20px;\">&nbsp;</div>"
                + "<table role=\"presentation\" class=\"db-panel\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#FAF7F2\" style=\"background-color:#FAF7F2;border:1px solid #EDE8DF;border-radius:9px;border-collapse:separate;\">"
                + details + "</table>";
        String callout = s.callout == null ? ""
                : "<div style=\"line-height:20px;font-size:20px;height:20px;\">&nbsp;</div>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#FAF7F2\" style=\"background-color:#FAF7F2;border:1px solid #EDE8DF;border-radius:9px;\">"
                + "<tr><td style=\"padding:14px 16px;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:14px;line-height:21px;color:#1C1917;\">"
                + escape(s.callout) + "</td></tr></table>";
        String otp = "";
        if (s.otpCode != null) {
            otp = "<div style=\"line-height:20px;font-size:20px;height:20px;\">&nbsp;</div>"
                    + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#FAF7F2\" class=\"db-panel\" style=\"background-color:#FAF7F2;border:1px solid #EDE8DF;border-radius:9px;border-collapse:separate;\">"
                    + "<tr><td align=\"center\" style=\"padding:20px 16px;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;\">"
                    + "<div class=\"db-mut\" style=\"font-size:11px;font-weight:700;letter-spacing:1px;color:#726B66;text-transform:uppercase;\">One-time access code</div>"
                    + "<div class=\"db-ink\" style=\"font-family:Consolas,'Courier New',monospace;font-size:32px;font-weight:700;letter-spacing:6px;color:#1C1917;padding:10px 0 6px 0;\">"
                    + escape(s.otpCode) + "</div>"
                    + (s.otpHint == null ? "" : "<div class=\"db-mut\" style=\"font-size:12px;color:#726B66;\">" + escape(s.otpHint) + "</div>")
                    + "</td></tr></table>";
        }
        String cta = "";
        if (s.ctaUrl != null && s.ctaLabel != null) {
            String url = escape(s.ctaUrl);
            String label = escape(s.ctaLabel);
            cta = "<div style=\"line-height:24px;font-size:24px;height:24px;\">&nbsp;</div>"
                    + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td align=\"left\">"
                    + "<!--[if mso]><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\""
                    + url + "\" style=\"height:46px;v-text-anchor:middle;width:250px;\" arcsize=\"14%\" stroke=\"f\" fillcolor=\""
                    + s.urgency.btn + "\"><w:anchorlock/><center style=\"color:#ffffff;font-family:'Segoe UI',Arial,sans-serif;font-size:15px;font-weight:600;\">"
                    + label + "</center></v:roundrect><![endif]-->"
                    + "<!--[if !mso]><!-- --><a class=\"db-btn\" href=\"" + url
                    + "\" style=\"background-color:" + s.urgency.btn
                    + ";border-radius:6px;color:#ffffff;display:inline-block;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:15px;font-weight:600;line-height:46px;text-align:center;text-decoration:none;padding:0 30px;mso-hide:all;\">"
                    + label + "</a><!--<![endif]--></td></tr></table>"
                    + (!s.showCtaUrl ? ""
                    : "<div style=\"line-height:10px;font-size:10px;height:10px;\">&nbsp;</div>"
                    + (s.ctaUrlLabel == null ? ""
                    : "<p class=\"db-mut\" style=\"margin:0 0 4px 0;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;line-height:18px;color:#726B66;\">"
                    + escape(s.ctaUrlLabel) + "</p>")
                    + "<p class=\"db-mut\" style=\"margin:0;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;line-height:18px;color:#726B66;word-break:break-all;\">"
                    + "<a href=\"" + url + "\" class=\"db-link db-a\" style=\"color:#92530A;text-decoration:underline;\">" + url + "</a></p>");
        }
        String after = s.afterCta == null ? ""
                : "<div style=\"line-height:14px;font-size:14px;height:14px;\">&nbsp;</div>"
                + "<p class=\"db-body\" style=\"margin:0;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:13px;line-height:20px;color:#726B66;\">"
                + escape(s.afterCta).replace("\n", "<br/><br/>") + "</p>";
        String accountLine = s.external
                ? "This link is unique to you. Please do not forward it.<br/>"
                + "<a href=\"mailto:support@arborgrc.com\" class=\"db-link db-a\" style=\"color:#92530A;text-decoration:underline;\">Contact support</a>"
                : (s.showAccountLine ? "You received this because you have an Arbor GRC account." : "");
        String footerCopy;
        if (why.isBlank()) {
            footerCopy = accountLine;
        } else if (accountLine.isBlank()) {
            footerCopy = why;
        } else {
            footerCopy = why + "<br/>" + accountLine;
        }
        return """
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office" lang="en">
                <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width,initial-scale=1"/>
                <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
                <meta name="x-apple-disable-message-reformatting"/>
                <meta name="color-scheme" content="light dark"/>
                <meta name="supported-color-schemes" content="light dark"/>
                <title>%s</title>
                <!--[if mso]><xml><o:OfficeDocumentSettings><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]-->
                <style type="text/css">
                body,table,td,a { -webkit-text-size-adjust:100%%; -ms-text-size-adjust:100%%; }
                table,td { mso-table-lspace:0pt; mso-table-rspace:0pt; }
                a { text-decoration:none; }
                .db-a { color:#92530A; }
                @media screen and (max-width:620px) {
                  .db-wrap { width:100%% !important; }
                  .db-pad { padding-left:20px !important; padding-right:20px !important; }
                  .db-btn { display:block !important; width:auto !important; text-align:center !important; }
                  .db-h1 { font-size:20px !important; line-height:27px !important; }
                }
                @media (prefers-color-scheme: dark) {
                  .db-page { background-color:#151311 !important; }
                  .db-card { background-color:#242019 !important; border-color:#443C31 !important; }
                  .db-panel { background-color:#2C2720 !important; border-color:#443C31 !important; }
                  .db-ink, .db-h1 { color:#F5F1EA !important; }
                  .db-body { color:#CFC7BA !important; }
                  .db-mut { color:#A49A8B !important; }
                  .db-brdr { border-color:#443C31 !important; }
                  .db-link, .db-a { color:#E8B44B !important; }
                  .db-mark { color:#E8B44B !important; }
                }
                </style>
                </head>
                <body class="db-page" style="margin:0;padding:0;width:100%%;background-color:#F5F1EA;" bgcolor="#F5F1EA">
                <div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;mso-hide:all;">%s</div>
                <table role="presentation" class="db-page" width="100%%" cellpadding="0" cellspacing="0" border="0" bgcolor="#F5F1EA" style="background-color:#F5F1EA;">
                <tr><td align="center" style="padding:28px 12px;">
                <table role="presentation" class="db-wrap" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:600px;">
                  <tr><td class="db-pad" style="padding:0 8px 12px 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                      <td align="left" style="font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:15px;font-weight:700;letter-spacing:2px;color:#92530A;" class="db-mark">ARBOR<span style="font-weight:400;">&nbsp;GRC</span></td>
                      <td align="right" class="db-mut" style="font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;color:#726B66;">%s</td>
                    </tr></table>
                  </td></tr>
                  <tr><td class="db-card" bgcolor="#FFFFFF" style="background-color:#FFFFFF;border:1px solid #EDE8DF;border-radius:12px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="border-collapse:separate;">
                      <tr><td bgcolor="%s" height="4" style="background-color:%s;height:4px;line-height:4px;font-size:0;border-radius:11px 11px 0 0;">&nbsp;</td></tr>
                      <tr><td class="db-pad db-tpad" style="padding:30px 34px 32px 34px;">
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr><td class="db-pill" bgcolor="%s" style="background-color:%s;border-radius:4px;padding:5px 10px;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:11px;font-weight:700;letter-spacing:0.8px;color:%s;line-height:1;">%s</td></tr></table>
                        <div style="line-height:14px;font-size:14px;height:14px;">&nbsp;</div>
                        %s<p class="db-body" style="margin:0;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:15px;line-height:23px;color:#44403C;">%s</p>
                        %s%s%s%s%s%s
                      </td></tr>
                    </table>
                  </td></tr>
                  %s
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(
                headline.isBlank() ? pill : headline,
                pre,
                workspace,
                s.urgency.bar, s.urgency.bar,
                s.urgency.pillBg, s.urgency.pillBg, s.urgency.pillFg, pill,
                headlineBlock(headline),
                lede,
                detailsTable + signOffHtml(s.note), callout, otp, cta, after, signOffHtml(s.signOff),
                s.showFooter ? footerHtml(footerCopy, workspace, trigger, mid, s.showMeta) : "");
    }

    private static String headlineBlock(String headline) {
        if (headline.isBlank()) {
            return "";
        }
        return "<h1 class=\"db-ink db-h1\" style=\"margin:0;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:22px;line-height:30px;font-weight:600;color:#1C1917;letter-spacing:-0.2px;\">"
                + headline + "</h1>\n"
                + "<div style=\"line-height:10px;font-size:10px;height:10px;\">&nbsp;</div>\n";
    }

    private static String signOffHtml(String signOff) {
        if (signOff == null) {
            return "";
        }
        return "<div style=\"line-height:20px;font-size:20px;height:20px;\">&nbsp;</div>"
                + "<p class=\"db-body\" style=\"margin:0;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:15px;line-height:23px;color:#44403C;\">"
                + escape(signOff).replace("\n", "<br/>") + "</p>";
    }

    private static String footerHtml(String footerCopy, String workspace, String trigger, String mid,
                                     boolean showMeta) {
        if (!showMeta) {
            if (footerCopy.isBlank()) {
                return "";
            }
            return """
                <tr><td class="db-pad" style="padding:18px 8px 0 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                      <tr><td class="db-mut" style="font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;line-height:19px;color:#726B66;">%s</td></tr>
                    </table>
                  </td></tr>""".formatted(footerCopy);
        }
        return """
                <tr><td class="db-pad" style="padding:18px 8px 0 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                      <tr><td class="db-mut" style="padding-bottom:14px;font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;line-height:19px;color:#726B66;">%s</td></tr>
                      <tr><td height="1" bgcolor="#EDE8DF" style="background-color:#EDE8DF;height:1px;font-size:0;line-height:0;">&nbsp;</td></tr>
                      <tr><td height="12" style="font-size:0;line-height:0;">&nbsp;</td></tr>
                      <tr><td class="db-mut" style="font-family:Poppins,'Segoe UI',Helvetica,Arial,sans-serif;font-size:11px;line-height:18px;color:#726B66;">
                        Arbor GRC · %s<br/>
                        Hosted in Microsoft Azure UAE North<br/>
                        Trigger <span style="font-family:Consolas,monospace;">%s</span> · Message ID <span style="font-family:Consolas,monospace;">%s</span>
                      </td></tr>
                    </table>
                  </td></tr>""".formatted(footerCopy, workspace, trigger, mid);
    }

    private static String plain(Spec s) {
        StringBuilder b = new StringBuilder();
        String top = s.headline.isBlank() ? s.pill : s.headline;
        b.append(top.toUpperCase()).append("\n");
        b.append("=".repeat(Math.min(62, Math.max(8, top.length())))).append("\n\n");
        if (!s.lede.isBlank()) {
            b.append(s.lede.replace("\n", "\n\n")).append("\n\n");
        }
        if (s.otpCode != null) {
            b.append("ONE-TIME ACCESS CODE: ").append(s.otpCode).append("\n");
            if (s.otpHint != null) {
                b.append(s.otpHint).append("\n");
            }
            b.append('\n');
        }
        for (Detail d : s.details) {
            b.append(d.label()).append(": ").append(d.value()).append('\n');
        }
        if (s.note != null) {
            b.append('\n').append(s.note).append('\n');
        }
        if (s.callout != null) {
            b.append('\n').append(s.callout).append('\n');
        }
        if (s.ctaUrl != null && s.ctaLabel != null) {
            b.append('\n').append(s.ctaLabel.toUpperCase()).append(":\n").append(s.ctaUrl).append('\n');
        }
        if (s.afterCta != null) {
            b.append('\n').append(s.afterCta.replace("\n", "\n\n")).append('\n');
        }
        if (s.signOff != null) {
            b.append('\n').append(s.signOff).append('\n');
        }
        if (!s.showFooter) {
            return b.toString();
        }
        b.append("\n--------------------------------------------------------------\n");
        if (!s.whyYouGotThis.isBlank()) {
            b.append(s.whyYouGotThis).append('\n');
        }
        if (s.external) {
            b.append("This link is unique to you. Please do not forward it.\n");
            b.append("Contact support: support@arborgrc.com\n");
        }
        if (s.showMeta) {
            b.append("\nArbor GRC · ").append(s.workspace).append('\n');
            b.append("Trigger ").append(s.triggerCode).append(" · Message ID ").append(s.messageId).append('\n');
        }
        return b.toString();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static String blankTo(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }
}
