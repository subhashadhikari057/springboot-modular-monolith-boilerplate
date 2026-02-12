package com.starterpack.backend.modules.auth.application;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.application.port.AuthEmailSenderPort;
import com.starterpack.backend.modules.auth.application.port.VerificationResendThrottlePort;
import com.starterpack.backend.modules.auth.api.dto.ConfirmVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.RequestVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ResetPasswordRequest;
import com.starterpack.backend.modules.auth.application.verification.VerificationChannelTargetResolver;
import com.starterpack.backend.modules.auth.application.verification.VerificationPurposeHandler;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import com.starterpack.backend.modules.users.infrastructure.VerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional
public class AuthVerificationService {
    private static final String LOCAL_PROVIDER = "local";

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuthProperties authProperties;
    private final AuthTokenService authTokenService;
    private final AuthEmailSenderPort authEmailSenderPort;
    private final VerificationResendThrottlePort verificationResendThrottlePort;
    private final Map<VerificationPurpose, VerificationPurposeHandler> purposeHandlers;
    private final Map<VerificationChannel, VerificationChannelTargetResolver> channelResolvers;

    public AuthVerificationService(
            VerificationRepository verificationRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            AuthProperties authProperties,
            AuthTokenService authTokenService,
            AuthEmailSenderPort authEmailSenderPort,
            VerificationResendThrottlePort verificationResendThrottlePort,
            List<VerificationPurposeHandler> purposeHandlers,
            List<VerificationChannelTargetResolver> channelResolvers
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.authProperties = authProperties;
        this.authTokenService = authTokenService;
        this.authEmailSenderPort = authEmailSenderPort;
        this.verificationResendThrottlePort = verificationResendThrottlePort;
        this.purposeHandlers = indexPurposeHandlers(purposeHandlers);
        this.channelResolvers = indexChannelResolvers(channelResolvers);
    }

    public IssuedVerificationData resendVerification(User user, RequestVerificationRequest request) {
        String cooldownKey = cooldownKey(user.getId().toString(), request.purpose(), request.channel());
        boolean acquired = verificationResendThrottlePort.acquire(cooldownKey, authProperties.getVerification().getResendCooldown());
        if (!acquired) {
            throw AppException.badRequest("Verification already requested recently. Please wait before retrying.");
        }
        return requestVerification(user, request);
    }

    public IssuedVerificationData requestAccountDeletionVerification(User user) {
        RequestVerificationRequest request = new RequestVerificationRequest(
                VerificationPurpose.ACCOUNT_DELETION,
                VerificationChannel.EMAIL,
                user.getEmail()
        );
        String cooldownKey = cooldownKey(user.getId().toString(), request.purpose(), request.channel());
        boolean acquired = verificationResendThrottlePort.acquire(cooldownKey, authProperties.getVerification().getResendCooldown());
        if (!acquired) {
            throw AppException.badRequest("Verification already requested recently. Please wait before retrying.");
        }
        return requestVerification(user, request);
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
        if (request.channel() == VerificationChannel.EMAIL) {
            authEmailSenderPort.sendVerificationEmail(new AuthEmailSenderPort.VerificationEmailCommand(
                    target,
                    user.getName(),
                    request.purpose(),
                    verification.getIdentifier(),
                    plainToken,
                    buildVerificationLink(verification.getIdentifier(), plainToken, request.purpose())
            ));
        }
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
        authEmailSenderPort.sendPasswordResetEmail(new AuthEmailSenderPort.PasswordResetEmailCommand(
                email,
                user.getName(),
                verification.getIdentifier(),
                plainToken,
                buildPasswordResetLink(verification.getIdentifier(), plainToken)
        ));
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

    public void consumeAccountDeletionToken(UUID userId, String token) {
        Verification verification = resolveVerification(
                userId.toString(),
                VerificationPurpose.ACCOUNT_DELETION,
                VerificationChannel.EMAIL,
                token
        );
        verification.setConsumedAt(OffsetDateTime.now());
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
                .orElseThrow(() -> AppException.badRequest("Invalid or expired token"));
    }

    private void applyVerificationEffect(Verification verification) {
        UUID userId = parseUuid(verification.getIdentifier(), "Invalid verification identifier");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.badRequest("Invalid verification identifier"));
        VerificationPurposeHandler handler = purposeHandlers.get(verification.getPurpose());
        if (handler == null) {
            throw AppException.badRequest("Unsupported verification purpose");
        }
        handler.apply(user, verification);
    }

    private String resolveTarget(User user, VerificationChannel channel, String target) {
        VerificationChannelTargetResolver resolver = channelResolvers.get(channel);
        if (resolver == null) {
            throw AppException.badRequest("Unsupported verification channel");
        }
        return resolver.resolveTarget(user, target);
    }

    private Map<VerificationPurpose, VerificationPurposeHandler> indexPurposeHandlers(List<VerificationPurposeHandler> handlers) {
        Map<VerificationPurpose, VerificationPurposeHandler> byPurpose = new EnumMap<>(VerificationPurpose.class);
        handlers.forEach(handler -> byPurpose.put(handler.purpose(), handler));
        return byPurpose;
    }

    private Map<VerificationChannel, VerificationChannelTargetResolver> indexChannelResolvers(
            List<VerificationChannelTargetResolver> resolvers
    ) {
        Map<VerificationChannel, VerificationChannelTargetResolver> byChannel = new EnumMap<>(VerificationChannel.class);
        resolvers.forEach(resolver -> byChannel.put(resolver.channel(), resolver));
        return byChannel;
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw AppException.badRequest(message);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildVerificationLink(String identifier, String token, VerificationPurpose purpose) {
        return UriComponentsBuilder.fromUriString(authProperties.getMail().getVerificationLinkBaseUrl())
                .queryParam("identifier", identifier)
                .queryParam("token", token)
                .queryParam("purpose", purpose.name())
                .build()
                .toUriString();
    }

    private String buildPasswordResetLink(String identifier, String token) {
        return UriComponentsBuilder.fromUriString(authProperties.getMail().getPasswordResetLinkBaseUrl())
                .queryParam("identifier", identifier)
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String cooldownKey(String identifier, VerificationPurpose purpose, VerificationChannel channel) {
        return authProperties.getCache().getPrefix() + ":verify:cooldown:" + identifier + ":" + purpose.name() + ":" + channel.name();
    }

    public record IssuedVerificationData(Verification verification, String token) {
    }
}
