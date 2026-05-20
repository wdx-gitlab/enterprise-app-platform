package com.ruijie.dapengine.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态查询排序规则。
 */
@Data
@NoArgsConstructor
public class SortSpec {

    /** 排序字段名 */
    private String field;

    /** 排序方向：ASC / DESC，默认 ASC */
    private String direction = "ASC";
}

