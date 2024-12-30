package com.ww.app.admin.constant;

/**
 * @author ww
 * @create 2024-09-19- 14:01
 * @description:
 */
public interface LogRecordConstants {

    // ======================= SYSTEM_USER 用户 =======================

    String SYSTEM_USER_TYPE = "SYSTEM 用户";
    String SYSTEM_USER_CREATE_SUB_TYPE = "创建用户";
    String SYSTEM_USER_CREATE_SUCCESS = "创建了用户【{{#user.realName}}】";
    String SYSTEM_USER_UPDATE_SUB_TYPE = "更新用户";
    String SYSTEM_USER_UPDATE_SUCCESS = "更新了用户【{{#user.realName}}】: {_DIFF{#form}}";
    String SYSTEM_USER_DELETE_SUB_TYPE = "删除用户";
    String SYSTEM_USER_DELETE_SUCCESS = "删除了用户【{{#user.realName}}】";
    String SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE = "重置用户密码";
    String SYSTEM_USER_UPDATE_PASSWORD_SUCCESS = "将用户【{{#user.realName}}】的密码从【{{#user.password}}】重置为【{{#newPassword}}】";

    // ======================= SYSTEM_ROLE 角色 =======================

    String SYSTEM_ROLE_TYPE = "SYSTEM 角色";
    String SYSTEM_ROLE_CREATE_SUB_TYPE = "创建角色";
    String SYSTEM_ROLE_CREATE_SUCCESS = "创建了角色【{{#role.name}}】";
    String SYSTEM_ROLE_UPDATE_SUB_TYPE = "更新角色";
    String SYSTEM_ROLE_UPDATE_SUCCESS = "更新了角色【{{#role.name}}】: {_DIFF{#form}}";
    String SYSTEM_ROLE_DELETE_SUB_TYPE = "删除角色";
    String SYSTEM_ROLE_DELETE_SUCCESS = "删除了角色【{{#role.name}}】";

}
