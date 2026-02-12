package com.starterpack.backend.modules.auth.infrastructure;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.application.port.AuthEmailSenderPort;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpAuthEmailSender implements AuthEmailSenderPort {
    private static final Logger logger = LoggerFactory.getLogger(SmtpAuthEmailSender.class);

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public SmtpAuthEmailSender(JavaMailSender mailSender, AuthProperties authProperties) {
        this.mailSender = mailSender;
        this.authProperties = authProperties;
    }

    @Override
    public void sendVerificationEmail(VerificationEmailCommand command) {
        String purposeLabel = toPurposeLabel(command.purpose());
        String subject = "Verify your " + purposeLabel;
        String body = "Hi " + safeName(command.recipientName()) + ",\n\n"
                + "Use this link to verify your " + purposeLabel + ":\n"
                + command.verificationLink() + "\n\n"
                + "If needed, manual token details are below:\n"
                + "identifier=" + command.identifier() + "\n"
                + "token=" + command.token() + "\n\n"
                + "This token expires soon.";
        send(command.recipientEmail(), subject, body);
    }

    @Override
    public void sendPasswordResetEmail(PasswordResetEmailCommand command) {
        String subject = "Reset your password";
        String body = "Hi " + safeName(command.recipientName()) + ",\n\n"
                + "Use this link to reset your password:\n"
                + command.resetLink() + "\n\n"
                + "If needed, manual token details are below:\n"
                + "identifier=" + command.identifier() + "\n"
                + "token=" + command.token() + "\n\n"
                + "If you did not request this, you can ignore this email.";
        send(command.recipientEmail(), subject, body);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authProperties.getMail().getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            logger.info("MAIL_SENT to={} subject={}", to, subject);
        } catch (MailException ex) {
            logger.warn("MAIL_SEND_FAILED to={} subject={} message={}", to, subject, ex.getMessage());
            throw AppException.serviceUnavailable("Unable to send email right now");
        }
    }

    private String toPurposeLabel(VerificationPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "email";
            case PHONE_VERIFICATION -> "phone";
            case PASSWORD_RESET -> "password";
        };
    }

    private String safeName(String name) {
        return (name == null || name.isBlank()) ? "there" : name;
    }
}
