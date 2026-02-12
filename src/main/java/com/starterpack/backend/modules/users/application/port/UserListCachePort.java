package com.starterpack.backend.modules.users.application.port;

import java.time.Duration;
import java.util.Optional;

import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.users.api.dto.UserResponse;

public interface UserListCachePort {
    Optional<PagedResponse<UserResponse>> getList(String listKey);

    void putList(String listKey, PagedResponse<UserResponse> response, Duration ttl);

    void invalidateLists();
}
