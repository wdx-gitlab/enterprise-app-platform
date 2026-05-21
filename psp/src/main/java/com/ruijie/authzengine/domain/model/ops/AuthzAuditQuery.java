package com.ruijie.authzengine.domain.model.ops;

import lombok.Builder;
import lombok.Data;

/**
 * 审计查询条件，用于审计日志分页检索。
 * <p>tenantId + appCode 为必填隔离条件，其余为可选过滤。</p>
 */
@Data
@Builder
public class AuthzAuditQuery {

    /** 租户标识（必填）。 */
    private String tenantId;

    /** 应用标识（必填）。 */
    private String appCode;

    /** 按主体模型过滤（如 SUB_USER），可选。 */
    private String subjectModel;

    /** 按主体 ID 过滤，可选。 */
    private String subjectId;

    /** 按资源模型过滤（如 RES_UI_MENU），可选。 */
    private String resourceModel;

    /** 按资源标识过滤，可选。 */
    private String resId;

    /** 按动作编码过滤，可选。 */
    private String actionCode;

    /** 按决策结果过滤（PERMIT / NOT_PERMIT / INDETERMINATE），可选。 */
    private String decision;

    /** 页码（1-based）。 */
    private int pageNo;

    /** 每页记录数。 */
    private int pageSize;
}
