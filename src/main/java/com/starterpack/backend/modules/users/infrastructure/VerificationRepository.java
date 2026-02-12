package com.starterpack.backend.modules.users.infrastructure;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import com.starterpack.backend.modules.users.domain.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, UUID> {
    Optional<Verification> findFirstByIdentifierAndPurposeAndChannelAndTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
            String identifier,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String tokenHash,
            OffsetDateTime now
    );

    void deleteByIdentifier(String identifier);
}
