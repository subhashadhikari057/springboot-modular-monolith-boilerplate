package com.starterpack.backend.common.error;

public class AppException extends RuntimeException {
    private final int status;
    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static AppException badRequest(String message) {
        return new AppException(400, "BAD_REQUEST", message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(401, "UNAUTHORIZED", message);
    }

    public static AppException notFound(String message) {
        return new AppException(404, "NOT_FOUND", message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, "CONFLICT", message);
    }
}
