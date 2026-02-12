package com.starterpack.backend.modules.auth.application;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.api.dto.ConfirmVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.RequestVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ResetPasswordRequest;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import com.starterpack.backend.modules.users.infrastructure.VerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthVerificationService {
    private static final String LOCAL_PROVIDER = "local";

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuthProperties authProperties;
    private final AuthTokenService authTokenService;

    public AuthVerificationService(
            VerificationRepository verificationRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            AuthProperties authProperties,
            AuthTokenService authTokenService
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.authProperties = authProperties;
        this.authTokenService = authTokenService;
    }

    public IssuedVerificationData requestVerification(User user, RequestVerificationRequest request) {
        String target = resolveTarget(user, request.channel(), request.target());
        Verification verification = new Verification();
        verification.setIdentifier(user.getId().toString());
        verification.setTarget(target);
        verification.setPurpose(request.purpose());
        verification.setChannel(request.channel());
        verification.setAttempts(0);
        verification.setMaxAttempts(5);

        String plainToken = authTokenService.generateToken();
        verification.setTokenHash(authTokenService.hashToken(plainToken));
        verification.setExpiresAt(OffsetDateTime.now().plus(authProperties.getVerification().getTtl()));
        verificationRepository.save(verification);
        return new IssuedVerificationData(verification, plainToken);
    }

    public void confirmVerification(ConfirmVerificationRequest request) {
        Verification verification = resolveVerification(
                request.identifier(),
                request.purpose(),
                request.channel(),
                request.token()
        );

        verification.setConsumedAt(OffsetDateTime.now());
        applyVerificationEffect(verification);
    }

    public Optional<IssuedVerificationData> forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        Optional<Account> accountOpt = accountRepository.findByProviderIdAndAccountId(LOCAL_PROVIDER, email);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }

        Verification verification = new Verification();
        verification.setIdentifier(user.getId().toString());
        verification.setTarget(email);
        verification.setPurpose(VerificationPurpose.PASSWORD_RESET);
        verification.setChannel(VerificationChannel.EMAIL);
        verification.setAttempts(0);
        verification.setMaxAttempts(5);

        String plainToken = authTokenService.generateToken();
        verification.setTokenHash(authTokenService.hashToken(plainToken));
        verification.setExpiresAt(OffsetDateTime.now().plus(authProperties.getVerification().getTtl()));
        verificationRepository.save(verification);
        return Optional.of(new IssuedVerificationData(verification, plainToken));
    }

    public UUID consumePasswordResetToken(ResetPasswordRequest request) {
        Verification verification = resolveVerification(
                request.identifier(),
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                request.token()
        );
        verification.setConsumedAt(OffsetDateTime.now());
        return parseUuid(request.identifier(), "Invalid identifier");
    }

    private Verification resolveVerification(
            String identifier,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String plainToken
    ) {
        return verificationRepository
                .findFirstByIdentifierAndPurposeAndChannelAndTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
                        identifier,
                        purpose,
                        channel,
                        authTokenService.hashToken(plainToken),
                        OffsetDateTime.now()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
    }

    private void applyVerificationEffect(Verification verification) {
        UUID userId = parseUuid(verification.getIdentifier(), "Invalid verification identifier");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification identifier"));

        if (verification.getPurpose() == VerificationPurpose.EMAIL_VERIFICATION) {
            if (!user.getEmail().equalsIgnoreCase(verification.getTarget())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification target does not match current email");
            }
            user.setEmailVerified(true);
        }

        if (verification.getPurpose() == VerificationPurpose.PHONE_VERIFICATION) {
            if (user.getPhone() == null || !user.getPhone().equals(verification.getTarget())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification target does not match current phone");
            }
            user.setPhoneVerified(true);
        }
    }

    private String resolveTarget(User user, VerificationChannel channel, String target) {
        if (target != null && !target.isBlank()) {
            return target.trim();
        }

        if (channel == VerificationChannel.EMAIL) {
            return user.getEmail();
        }

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone target is required");
        }
        return user.getPhone();
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record IssuedVerificationData(Verification verification, String token) {
    }
}
