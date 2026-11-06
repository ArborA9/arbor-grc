package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateAssessmentRequest(
        Long vendorId,
        @Email @Size(max = 255) String spocEmail,
        LocalDate dueDate,
        Integer expiryDays,
        @Size(max = 255) String title,
        List<@Email String> extraEmails
) {
}
