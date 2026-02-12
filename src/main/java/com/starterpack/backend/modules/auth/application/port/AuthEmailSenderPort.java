package com.starterpack.backend.modules.auth.application.port;

import com.starterpack.backend.modules.users.domain.VerificationPurpose;

public interface AuthEmailSenderPort {
    void sendVerificationEmail(VerificationEmailCommand command);

    void sendPasswordResetEmail(PasswordResetEmailCommand command);

    record VerificationEmailCommand(
            String recipientEmail,
            String recipientName,
            VerificationPurpose purpose,
            String identifier,
            String token,
            String verificationLink
    ) {
    }

    record PasswordResetEmailCommand(
            String recipientEmail,
            String recipientName,
            String identifier,
            String token,
            String resetLink
    ) {
    }
}
