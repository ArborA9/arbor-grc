package com.a9.arbor.grc.security;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtServiceTest {

    @Test
    void vendorSessionJwtCarriesInviteId() {
        ArborSecurityProperties props = new ArborSecurityProperties();
        props.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        JwtService jwt = new JwtService(props);
        User user = new User();
        user.setEmail("vendor@example.com");
        String vendorJwt = jwt.issueForVendorSession(user, "tenant-a", List.of("VENDOR"), 30, 77L);
        Claims claims = jwt.parse(vendorJwt);
        assertEquals(77L, JwtService.vendorTokenId(claims));
        assertEquals(JwtService.PURPOSE_VENDOR_PORTAL, claims.get(JwtService.CLAIM_PURPOSE, String.class));
        String staffJwt = jwt.issueForUser(user, "tenant-a", List.of("ADMIN"), 30);
        assertNull(JwtService.vendorTokenId(jwt.parse(staffJwt)));
    }
}
