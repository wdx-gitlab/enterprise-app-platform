package com.ruijie.authzengine.application.sdk.model;

/**
 * SDK 侧主体关系类型枚举。
 */
public enum GovernanceRelationType {

    /** 用户或主体与角色的关系。 */
    ROLE,

    /** 用户或主体与组织的关系。 */
    ORG,

    /** 用户或主体与岗位的关系。 */
    POSITION,

    /** 用户或主体与用户组的关系。 */
    GROUP
}