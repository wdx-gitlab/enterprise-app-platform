package com.ruijie.authzengine.domain.model.governance;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 治理分页结果，用于治理层所有分页查询场景的统一返回封装。
 *
 * @param <T> 记录类型，如 AuthMetaModelDefinition、AuthPermissionItem 等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页码（1-based）。 */
    private int pageNo;

    /** 每页记录数。 */
    private int pageSize;

    /** 符合条件的总记录数。 */
    private long total;

    /** 当前页数据记录列表。 */
    private List<T> records;
}