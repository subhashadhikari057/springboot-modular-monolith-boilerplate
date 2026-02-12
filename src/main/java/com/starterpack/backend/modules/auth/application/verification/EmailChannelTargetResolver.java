package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.VerificationChannel;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelTargetResolver implements VerificationChannelTargetResolver {
    @Override
    public VerificationChannel channel() {
        return VerificationChannel.EMAIL;
    }

    @Override
    public String resolveTarget(User user, String requestedTarget) {
        if (requestedTarget != null && !requestedTarget.isBlank()) {
            return requestedTarget.trim();
        }
        return user.getEmail();
    }
}
