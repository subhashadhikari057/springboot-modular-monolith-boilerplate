package com.starterpack.backend.modules.users.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.common.web.PageMeta;
import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.config.CacheProperties;
import com.starterpack.backend.modules.auth.application.port.AuthSessionCachePort;
import com.starterpack.backend.modules.auth.application.AuthService;
import com.starterpack.backend.modules.users.api.dto.CreateUserRequest;
import com.starterpack.backend.modules.users.api.dto.RoleSummary;
import com.starterpack.backend.modules.users.api.dto.UpdateUserRequest;
import com.starterpack.backend.modules.users.api.dto.UserPermissionsResponse;
import com.starterpack.backend.modules.users.api.dto.UserResponse;
import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.application.port.UserListCachePort;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.UserStatus;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import com.starterpack.backend.modules.users.infrastructure.VerificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {
    private static final String LOCAL_PROVIDER = "local";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final VerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserListCachePort userListCache;
    private final SessionRepository sessionRepository;
    private final AuthSessionCachePort authSessionCache;
    private final CacheProperties cacheProperties;
    private final AuthService authService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            VerificationRepository verificationRepository,
            PasswordEncoder passwordEncoder,
            UserListCachePort userListCache,
            SessionRepository sessionRepository,
            AuthSessionCachePort authSessionCache,
            CacheProperties cacheProperties,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.verificationRepository = verificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.userListCache = userListCache;
        this.sessionRepository = sessionRepository;
        this.authSessionCache = authSessionCache;
        this.cacheProperties = cacheProperties;
        this.authService = authService;
    }

    public User createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw AppException.conflict("Email already in use");
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
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setProviderId(LOCAL_PROVIDER);
        account.setAccountId(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        accountRepository.save(account);

        userListCache.invalidateLists();
        return user;
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(
            int page,
            int size,
            String sortBy,
            Sort.Direction sortDirection,
            String q,
            Integer roleId,
            Boolean emailVerified
    ) {
        String listCacheKey = listCacheKey(page, size, sortBy, sortDirection, q, roleId, emailVerified);
        Optional<PagedResponse<UserResponse>> cached = userListCache.getList(listCacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<User> users = userRepository.findAll(buildUserFilter(q, roleId, emailVerified), pageable);

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
                .orElseThrow(() -> AppException.notFound("Role not found"));
        user.setRole(role);
        sessionRepository.deleteByUserId(user.getId());
        authSessionCache.evictAllUserSessions(user.getId());
        userListCache.invalidateLists();
        return user;
    }

    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = getUser(userId);
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }
        if (request.image() != null) {
            user.setImage(trimToNull(request.image()));
        }
        if (request.roleId() != null) {
            Role role = resolveRole(request.roleId());
            user.setRole(role);
            sessionRepository.deleteByUserId(user.getId());
            authSessionCache.evictAllUserSessions(user.getId());
        }
        if (request.emailVerified() != null) {
            user.setEmailVerified(request.emailVerified());
        }
        if (request.phoneVerified() != null) {
            user.setPhoneVerified(request.phoneVerified());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        userListCache.invalidateLists();
        return user;
    }

    public User updateUserStatus(UUID userId, UserStatus status) {
        User user = getUser(userId);
        user.setStatus(status);
        if (status != UserStatus.ACTIVE) {
            sessionRepository.deleteByUserId(user.getId());
            authSessionCache.evictAllUserSessions(user.getId());
        }
        userListCache.invalidateLists();
        return user;
    }

    @Transactional(readOnly = true)
    public UserPermissionsResponse getUserPermissions(UUID userId) {
        User user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        List<String> permissions = user.getRole() == null
                ? List.of()
                : user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .sorted()
                .toList();
        return new UserPermissionsResponse(user.getId(), RoleSummary.from(user.getRole()), permissions);
    }

    public void requestPasswordResetForUser(UUID userId) {
        User user = getUser(userId);
        authService.requestPasswordResetByEmail(user.getEmail());
    }

    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        verificationRepository.deleteByIdentifier(user.getId().toString());
        userListCache.invalidateLists();
        userRepository.delete(user);
    }

    private Role resolveRole(Integer roleId) {
        if (roleId != null) {
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> AppException.notFound("Role not found"));
        }

        return roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> AppException.badRequest("Default role USER not found"));
    }

    private void cacheUserList(String listCacheKey, PagedResponse<UserResponse> response) {
        userListCache.putList(listCacheKey, response, cacheProperties.getUsers().getListTtl());
    }

    private String listCacheKey(
            int page,
            int size,
            String sortBy,
            Sort.Direction sortDirection,
            String q,
            Integer roleId,
            Boolean emailVerified
    ) {
        String qPart = q == null || q.isBlank() ? "any-q" : "q-" + q.trim().toLowerCase(Locale.ROOT);
        String rolePart = roleId == null ? "any-role" : "role-" + roleId;
        String verifiedPart = emailVerified == null ? "any-verified" : "emailVerified-" + emailVerified;
        return "users:list:" + page + ":" + size + ":" + sortBy + ":" + sortDirection.name().toLowerCase()
                + ":" + qPart + ":" + rolePart + ":" + verifiedPart;
    }

    private Specification<User> buildUserFilter(String q, Integer roleId, Boolean emailVerified) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("id"), roleId));
            }
            if (emailVerified != null) {
                predicates.add(cb.equal(root.get("emailVerified"), emailVerified));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
