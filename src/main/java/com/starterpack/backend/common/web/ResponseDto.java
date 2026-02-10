package com.starterpack.backend.common.web;

public record ResponseDto<T>(
        String message,
        T data
) {
}
