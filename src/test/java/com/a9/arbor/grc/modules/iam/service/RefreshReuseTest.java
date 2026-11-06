package com.a9.arbor.grc.modules.iam.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefreshReuseTest {

    private final Instant now = Instant.parse("2026-09-23T00:00:00Z");

    @Test
    void liveTokenRotates() {
        assertEquals(RefreshReuse.Decision.ROTATE,
                RefreshReuse.decide(now, now.plusSeconds(3600), null, null));
    }

    @Test
    void expiredTokenIsRejected() {
        assertEquals(RefreshReuse.Decision.REJECT,
                RefreshReuse.decide(now, now.minusSeconds(1), null, null));
    }

    @Test
    void rotationReplayInsideGraceReissues() {
        assertEquals(RefreshReuse.Decision.GRACE_REISSUE,
                RefreshReuse.decide(now, now.plusSeconds(3600), now.minusSeconds(10), RefreshReuse.ROTATED));
    }

    @Test
    void rotationReplayAfterGraceRevokesAll() {
        assertEquals(RefreshReuse.Decision.REUSE_REVOKE_ALL,
                RefreshReuse.decide(now, now.plusSeconds(3600), now.minusSeconds(120), RefreshReuse.ROTATED));
    }

    @Test
    void logoutReplayDoesNotRevokeOthers() {
        assertEquals(RefreshReuse.Decision.REJECT,
                RefreshReuse.decide(now, now.plusSeconds(3600), now.minusSeconds(5), RefreshReuse.LOGOUT));
    }
}
