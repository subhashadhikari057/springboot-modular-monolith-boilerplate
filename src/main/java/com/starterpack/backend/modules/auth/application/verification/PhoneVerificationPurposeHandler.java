package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import org.springframework.stereotype.Component;

@Component
public class PhoneVerificationPurposeHandler implements VerificationPurposeHandler {
    @Override
    public VerificationPurpose purpose() {
        return VerificationPurpose.PHONE_VERIFICATION;
    }

    @Override
    public void apply(User user, Verification verification) {
        if (user.getPhone() == null || !user.getPhone().equals(verification.getTarget())) {
            throw AppException.badRequest("Verification target does not match current phone");
        }
        user.setPhoneVerified(true);
    }
}
