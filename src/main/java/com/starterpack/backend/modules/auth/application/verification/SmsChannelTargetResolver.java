package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.VerificationChannel;
import org.springframework.stereotype.Component;

@Component
public class SmsChannelTargetResolver implements VerificationChannelTargetResolver {
    @Override
    public VerificationChannel channel() {
        return VerificationChannel.SMS;
    }

    @Override
    public String resolveTarget(User user, String requestedTarget) {
        if (requestedTarget != null && !requestedTarget.isBlank()) {
            return requestedTarget.trim();
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw AppException.badRequest("Phone target is required");
        }
        return user.getPhone();
    }
}
