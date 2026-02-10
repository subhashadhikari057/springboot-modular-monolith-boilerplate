package com.starterpack.backend.modules.users.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.starterpack.backend.common.web.PageMeta;
import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.users.api.dto.CreateUserRequest;
import com.starterpack.backend.modules.users.api.dto.UserResponse;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
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

        return user;
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(int page, int size, String sortBy, Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<User> users = userRepository.findAll(pageable);

        List<UserResponse> items = users.getContent().stream()
                .map(UserResponse::from)
                .toList();

        return new PagedResponse<>(items, PageMeta.from(users));
    }

    public User updateUserRole(UUID userId, Integer roleId) {
        User user = getUser(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        user.setRole(role);
        return user;
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
}
