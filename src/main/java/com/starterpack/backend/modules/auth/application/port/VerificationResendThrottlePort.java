package com.starterpack.backend.modules.auth.application.port;

import java.time.Duration;

public interface VerificationResendThrottlePort {
    boolean acquire(String key, Duration cooldown);
}
