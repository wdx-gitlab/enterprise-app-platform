package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * apply-schema 操作的结果模型。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemaChangeResult {
    /** Subject code，如 CUSTOMER */
    private String subject;
    /** 动态表名，如 dap_customer */
    private String table;
    /** 本次实际执行的 DDL 语句列表；幂等时为空列表 */
    private List<String> executedDdl;
}
