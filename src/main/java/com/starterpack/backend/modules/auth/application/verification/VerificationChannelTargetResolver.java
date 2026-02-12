package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.VerificationChannel;

public interface VerificationChannelTargetResolver {
    VerificationChannel channel();

    String resolveTarget(User user, String requestedTarget);
}
