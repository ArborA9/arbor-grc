package com.a9.arbor.grc.modules.notification.mail;

import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Wave 1 TPRM mail: A8 assign/OTP, A3 return, A10 close. No OTP on the assign mail. */
@Service
public class TprmNotificationMailComposer {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);
    private static final DateTimeFormatter INSTANT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final TenantRepository tenantRepository;

    public TprmNotificationMailComposer(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public NotificationMailContent assign(String vendorName, String projectTitle, String displayId,
                                          String questionnaireName, LocalDate dueDate, Instant expiresAt,
                                          String accessLink) {
        String workspace = workspace();
        String due = formatDay(dueDate);
        String expires = formatInstant(expiresAt);
        String title = blank(projectTitle, displayId);
        String subject = due == null
                ? "Action: " + title + " has been assigned"
                : "Action: " + title + " - response due " + due;
        String preheader = join(" · ", vendorName, questionnaireName,
                expires == null ? null : "link expires " + expires);
        return NotificationMailRenderer.spec()
                .workspace(workspace)
                .preheader(preheader)
                .pill("SECURE LINK")
                .urgency(NotificationMailRenderer.Urgency.ACTION)
                .headline("You have been asked to complete a Third-Party Assessment Project")
                .lede(workspace + " uses Arbor GRC to assess vendors. You do not need an account. Open the link and enter the email address that received the invitation. A one-time verification code will be sent to that email address.")
                .detail("Vendor", vendorName)
                .detail("Project", title)
                .detail("Questionnaire", questionnaireName)
                .detail("Respond by", due)
                .detail("Link expires", expires)
                .cta("Open the questionnaire", accessLink)
                .afterCta("The passcode will be sent only to the invited email address. Your progress saves. You can close the questionnaire and return using the same link.")
                .triggerCode("VENDOR_QUESTIONNAIRE_SENT")
                .external(true)
                .render(subject);
    }

    public NotificationMailContent returned(String vendorName, String projectTitle, String displayId,
                                            String questionnaireName, Instant expiresAt, String accessLink) {
        String workspace = workspace();
        String title = blank(projectTitle, displayId);
        String expires = formatInstant(expiresAt);
        return NotificationMailRenderer.spec()
                .workspace(workspace)
                .preheader(join(" · ", vendorName, "returned controls", expires == null ? null : "link expires " + expires))
                .pill("RETURNED")
                .urgency(NotificationMailRenderer.Urgency.RETURNED)
                .headline("Some controls were returned for update")
                .lede("Update only the returned controls on " + title + ". Open the same link and enter the email address that received the invitation. A one-time verification code will be sent to that email address.")
                .detail("Vendor", vendorName)
                .detail("Project", title)
                .detail("Questionnaire", questionnaireName)
                .detail("Link expires", expires)
                .callout("Each returned control must be updated before you submit again.")
                .cta("Open the questionnaire", accessLink)
                .afterCta("The passcode will be sent only to the invited email address. Use the same link.")
                .triggerCode("VENDOR_QUESTIONNAIRE_RETURNED")
                .external(true)
                .render("Action: Questionnaire returned - " + title);
    }

    public NotificationMailContent otp(String vendorName, String projectTitle, String displayId,
                                       String otpCode, Instant expiresAt) {
        String workspace = workspace();
        String title = blank(projectTitle, displayId);
        String expires = formatInstant(expiresAt);
        return NotificationMailRenderer.spec()
                .workspace(workspace)
                .preheader(join(" · ", title, "passcode"))
                .pill("SECURE LINK")
                .urgency(NotificationMailRenderer.Urgency.ACTION)
                .headline("Your passcode")
                .lede("Enter the one-time verification code sent to the invited email address. Arbor will never ask you for this passcode by phone or reply email.")
                .detail("Vendor", vendorName)
                .detail("Project", title)
                .otp(otpCode, expires == null
                        ? "Arbor will never ask you for this passcode by phone or reply email."
                        : "Valid until " + expires + ". Arbor will never ask you for this passcode by phone or reply email.")
                .triggerCode("VENDOR_QUESTIONNAIRE_OTP")
                .external(true)
                .render("Action: Your passcode");
    }

    public NotificationMailContent closed(String vendorName, String projectTitle, String displayId,
                                          boolean compliant, String posturePercent) {
        String workspace = workspace();
        String title = blank(projectTitle, displayId);
        String outcome = compliant ? "Compliant" : "Not compliant";
        String posture = posturePercent == null || posturePercent.isBlank() ? null : posturePercent.trim() + "%";
        String subject = "FYI: " + title + " is complete - " + outcome;
        return NotificationMailRenderer.spec()
                .workspace(workspace)
                .preheader(join(" · ", posture == null ? null : "Posture target " + posture, "no action needed from you"))
                .pill("FOR INFORMATION")
                .urgency(NotificationMailRenderer.Urgency.INFO)
                .headline(title + " is complete")
                .lede("The project is complete. Outcome: " + outcome + ". No action is needed from you.")
                .detail("Vendor", vendorName)
                .detail("Project", title)
                .detail("Posture target", posture)
                .detail("Outcome", outcome)
                .triggerCode("VENDOR_PROJECT_CLOSED")
                .external(true)
                .render(subject);
    }

    private String workspace() {
        String id = TenantContext.getTenantId();
        if (id == null || id.isBlank()) {
            return "Arbor GRC";
        }
        return tenantRepository.findByTenantId(id).map(t -> {
            String name = t.getName();
            return name == null || name.isBlank() ? id : name.trim();
        }).orElse(id);
    }

    static String formatDay(LocalDate day) {
        return day == null ? null : DAY.format(day);
    }

    static String formatInstant(Instant when) {
        return when == null ? null : INSTANT.format(when);
    }

    private static String blank(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback == null ? "" : fallback;
    }

    private static String join(String sep, String... parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            if (!b.isEmpty()) {
                b.append(sep);
            }
            b.append(p.trim());
        }
        return b.toString();
    }
}
