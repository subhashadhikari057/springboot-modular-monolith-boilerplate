package com.starterpack.backend.modules.audit.application;

public final class AuditActions {
    private AuditActions() {
    }

    public static final String AUTH_REGISTER_SUCCESS = "auth.register.success";
    public static final String AUTH_REGISTER_FAILURE = "auth.register.failure";
    public static final String AUTH_LOGIN_SUCCESS = "auth.login.success";
    public static final String AUTH_LOGIN_FAILURE = "auth.login.failure";
    public static final String AUTH_LOGOUT = "auth.logout";
    public static final String AUTH_REFRESH_SUCCESS = "auth.refresh.success";
    public static final String AUTH_REFRESH_FAILURE = "auth.refresh.failure";
    public static final String AUTH_REAUTH_SUCCESS = "auth.reauth.success";
    public static final String AUTH_REAUTH_FAILURE = "auth.reauth.failure";
    public static final String AUTH_LOGOUT_ALL = "auth.logout_all";
    public static final String AUTH_SESSION_REVOKE = "auth.session.revoke";
    public static final String AUTH_VERIFY_REQUEST = "auth.verify.request";
    public static final String AUTH_VERIFY_RESEND = "auth.verify.resend";
    public static final String AUTH_VERIFY_CONFIRM = "auth.verify.confirm";
    public static final String AUTH_PASSWORD_FORGOT_REQUEST = "auth.password.forgot.request";
    public static final String AUTH_PASSWORD_RESET = "auth.password.reset";
    public static final String AUTH_PASSWORD_CHANGE = "auth.password.change";
    public static final String AUTH_ACCOUNT_DELETE_REQUEST = "auth.account.delete.request";
    public static final String AUTH_ACCOUNT_DELETE_CONFIRM = "auth.account.delete.confirm";

    public static final String USERS_CREATE = "users.create";
    public static final String USERS_UPDATE = "users.update";
    public static final String USERS_DELETE = "users.delete";
    public static final String USERS_ROLE_UPDATE = "users.role.update";
    public static final String USERS_STATUS_UPDATE = "users.status.update";
    public static final String USERS_PASSWORD_RESET_REQUEST = "users.password.reset.request";

    public static final String ROLES_CREATE = "roles.create";
    public static final String ROLES_PERMISSIONS_UPDATE = "roles.permissions.update";
    public static final String PERMISSIONS_CREATE = "permissions.create";
}
