package com.starterpack.backend.modules.users.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.starterpack.backend.common.web.PageMeta;
import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.auth.infrastructure.AuthSessionCache;
import com.starterpack.backend.modules.users.api.dto.CreateUserRequest;
import com.starterpack.backend.modules.users.api.dto.UserResponse;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserCache;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import com.starterpack.backend.modules.users.infrastructure.VerificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {
    private static final String LOCAL_PROVIDER = "local";
    private static final java.time.Duration USER_CACHE_TTL = java.time.Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final VerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCache userCache;
    private final SessionRepository sessionRepository;
    private final AuthSessionCache authSessionCache;
    private final ObjectMapper objectMapper;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            VerificationRepository verificationRepository,
            PasswordEncoder passwordEncoder,
            UserCache userCache,
            SessionRepository sessionRepository,
            AuthSessionCache authSessionCache,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.verificationRepository = verificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCache = userCache;
        this.sessionRepository = sessionRepository;
        this.authSessionCache = authSessionCache;
        this.objectMapper = objectMapper;
    }

    public User createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        });

        Role role = resolveRole(request.roleId());

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setImage(request.image());
        user.setRole(role);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user = userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setProviderId(LOCAL_PROVIDER);
        account.setAccountId(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        accountRepository.save(account);

        userCache.invalidateLists();
        return user;
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(int page, int size, String sortBy, Sort.Direction sortDirection) {
        String listCacheKey = listCacheKey(page, size, sortBy, sortDirection);
        String cached = userCache.getList(listCacheKey);
        if (cached != null) {
            userCache.logHit(listCacheKey);
            PagedResponse<UserResponse> response = readPagedUserResponse(cached);
            if (response != null) {
                return response;
            }
            userCache.invalidateLists();
        }

        userCache.logMiss(listCacheKey);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<User> users = userRepository.findAll(pageable);

        List<UserResponse> items = users.getContent().stream()
                .map(UserResponse::from)
                .toList();

        PagedResponse<UserResponse> response = new PagedResponse<>(items, PageMeta.from(users));
        cacheUserList(listCacheKey, response);
        return response;
    }

    public User updateUserRole(UUID userId, Integer roleId) {
        User user = getUser(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        user.setRole(role);
        sessionRepository.deleteByUserId(user.getId());
        authSessionCache.evictAllUserSessions(user.getId());
        userCache.invalidateLists();
        return user;
    }

    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        verificationRepository.deleteByIdentifier(user.getId().toString());
        userCache.invalidateLists();
        userRepository.delete(user);
    }

    private Role resolveRole(Integer roleId) {
        if (roleId != null) {
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        }

        return roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Default role USER not found"
                ));
    }

    private void cacheUserList(String listCacheKey, PagedResponse<UserResponse> response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            userCache.putList(listCacheKey, json, USER_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
            // Cache failures should not affect request flow.
        }
    }

    private PagedResponse<UserResponse> readPagedUserResponse(String cached) {
        try {
            return objectMapper.readValue(
                    cached,
                    objectMapper.getTypeFactory().constructParametricType(PagedResponse.class, UserResponse.class)
            );
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String listCacheKey(int page, int size, String sortBy, Sort.Direction sortDirection) {
        return "users:list:" + page + ":" + size + ":" + sortBy + ":" + sortDirection.name().toLowerCase();
    }
}
