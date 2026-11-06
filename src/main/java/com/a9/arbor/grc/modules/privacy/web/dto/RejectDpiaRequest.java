package com.a9.arbor.grc.modules.privacy.web.dto;

import jakarta.validation.constraints.Size;

public record RejectDpiaRequest(@Size(max = 2000) String notes) {
}
