package com.a9.arbor.grc.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PasswordPolicyServiceTest {

    private static PasswordPolicyService policy(boolean strict) {
        ArborSecurityProperties props = new ArborSecurityProperties();
        props.getPassword().setStrict(strict);
        return new PasswordPolicyService(props);
    }

    @Test
    void strictRequiresSpecialCharacter() {
        PasswordPolicyService strict = policy(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> strict.assertValid("Aa1234567"));
        assertTrue(ex.getReason().contains("special character"));
        assertDoesNotThrow(() -> strict.assertValid("Aa12345!"));
        assertThrows(ResponseStatusException.class, () -> strict.assertValid("Aa 123456"));
        assertTrue(strict.specialCharRequired());
    }

    @Test
    void relaxedKeepsLengthOnly() {
        PasswordPolicyService relaxed = policy(false);
        assertDoesNotThrow(() -> relaxed.assertValid("aaaaaaaa"));
        assertThrows(ResponseStatusException.class, () -> relaxed.assertValid("short"));
    }
}
