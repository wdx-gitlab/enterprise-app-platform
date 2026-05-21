package com.ruijie.authzengine.domain.model.ops;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鉴权审计分页结果，封装审计日志查询的分页返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzAuditPage {

    /** 当前页审计记录列表。 */
    private List<AuthzAuditRecord> records;

    /** 当前页码（1-based）。 */
    private int pageNo;

    /** 每页记录数。 */
    private int pageSize;

    /** 符合条件的总记录数。 */
    private long total;
}