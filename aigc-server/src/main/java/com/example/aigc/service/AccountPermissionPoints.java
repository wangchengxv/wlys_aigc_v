package com.example.aigc.service;

public final class AccountPermissionPoints {
    private AccountPermissionPoints() {
    }

    public static final String MENU_ACCOUNT_DIRECTORY = "menu:account:directory:view";

    public static final String API_ORG_UNIT_LIST = "api:account:org-unit:list";
    public static final String API_ORG_UNIT_CREATE = "api:account:org-unit:create";

    public static final String API_USER_LIST = "api:account:user:list";
    public static final String API_USER_CREATE = "api:account:user:create";
    public static final String API_USER_PROFILE_UPDATE = "api:account:user:profile:update";
    public static final String API_USER_STATUS_UPDATE = "api:account:user:status:update";
    public static final String API_USER_PASSWORD_RESET = "api:account:user:password:reset";
    public static final String API_USER_ROLE_UPDATE = "api:account:user:role:update";
    public static final String API_USER_LOCK_UPDATE = "api:account:user:lock:update";
    public static final String API_USER_FORCE_LOGOUT = "api:account:user:force-logout";

    public static final String API_USER_BATCH_STATUS = "api:account:user:batch:status";
    public static final String API_USER_BATCH_LOCK = "api:account:user:batch:lock";
    public static final String API_USER_BATCH_ROLE = "api:account:user:batch:role";

    public static final String API_USER_IMPORT_TEMPLATE = "api:account:user:import-template:download";
    public static final String API_USER_IMPORT = "api:account:user:import";
    public static final String API_USER_IMPORT_TASK_QUERY = "api:account:user:import-task:query";
    public static final String API_USER_EXPORT = "api:account:user:export";
    public static final String API_USER_BATCH_STATS = "api:account:user:batch:stats";
}
