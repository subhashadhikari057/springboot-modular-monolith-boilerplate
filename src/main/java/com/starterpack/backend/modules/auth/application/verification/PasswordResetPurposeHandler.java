package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetPurposeHandler implements VerificationPurposeHandler {
    @Override
    public VerificationPurpose purpose() {
        return VerificationPurpose.PASSWORD_RESET;
    }

    @Override
    public void apply(User user, Verification verification) {
        // Intentionally no-op. Password reset effect is handled in reset password flow.
    }
}
