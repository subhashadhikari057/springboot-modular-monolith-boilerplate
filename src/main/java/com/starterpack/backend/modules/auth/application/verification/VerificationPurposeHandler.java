package com.starterpack.backend.modules.auth.application.verification;

import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;

public interface VerificationPurposeHandler {
    VerificationPurpose purpose();

    void apply(User user, Verification verification);
}
