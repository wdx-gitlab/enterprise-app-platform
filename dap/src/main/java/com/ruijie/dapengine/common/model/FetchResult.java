package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DataProvider.fetch() 的返回值。
 *
 * <p>records 为原始字段名 Map 列表（未经字段映射），由 FieldMappingService 进行列名转换。</p>
 */
@Data
@AllArgsConstructor
public class FetchResult {

    private List<Map<String, Object>> records;

    public static FetchResult of(List<Map<String, Object>> records) {
        return new FetchResult(records == null ? Collections.emptyList() : records);
    }

    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }
}
