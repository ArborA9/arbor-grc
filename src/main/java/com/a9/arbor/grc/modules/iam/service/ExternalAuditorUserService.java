package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class ExternalAuditorUserService {

    private final UserRepository userRepository;

    public ExternalAuditorUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds an existing external auditor by email or creates a passwordless LOCAL user
     * (same behaviour as OTP invite redemption).
     */
    public User resolveOrCreate(String email, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "External auditor email is required");
        }
        String name = fullName == null ? "" : fullName.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "External auditor name is required");
        }
        return userRepository.findByEmail(normalizedEmail)
                .map(existing -> {
                    if (!existing.getRoles().contains(Role.EXTERNAL_AUDITOR)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Email belongs to an existing user with a different role: use standard login");
                    }
                    if (!name.equals(existing.getFullName())) {
                        existing.setFullName(name);
                        return userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(normalizedEmail);
                    u.setFullName(name);
                    u.setAuthType(AuthType.LOCAL);
                    u.setStatus(UserStatus.ACTIVE);
                    u.setRoles(Set.of(Role.EXTERNAL_AUDITOR));
                    return userRepository.save(u);
                });
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
