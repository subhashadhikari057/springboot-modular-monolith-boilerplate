package com.starterpack.backend.modules.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByProviderIdAndAccountId(String providerId, String accountId);

    Optional<Account> findByUserIdAndProviderId(UUID userId, String providerId);
}
