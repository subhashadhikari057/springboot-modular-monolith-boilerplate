package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationPurposeHandler implements VerificationPurposeHandler {
    @Override
    public VerificationPurpose purpose() {
        return VerificationPurpose.EMAIL_VERIFICATION;
    }

    @Override
    public void apply(User user, Verification verification) {
        if (!user.getEmail().equalsIgnoreCase(verification.getTarget())) {
            throw AppException.badRequest("Verification target does not match current email");
        }
        user.setEmailVerified(true);
    }
}
