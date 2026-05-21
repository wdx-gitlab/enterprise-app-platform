package com.ruijie.authzengine.application.sdk.model;

/**
 * SDK 可写入的主体模型枚举。
 */
public enum GovernanceSubjectModel {

    /** 用户主体。 */
    SUB_USER,

    /** 角色主体。 */
    SUB_ROLE,

    /** 组织主体。 */
    SUB_ORG,

    /** 岗位主体。 */
    SUB_POSITION,

    /** 用户组主体。 */
    SUB_GROUP
}