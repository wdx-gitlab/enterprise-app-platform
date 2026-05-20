package com.ruijie.dapengine.sync;

import com.ruijie.dapengine.common.model.FieldMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段映射服务：将 DataProvider 返回的源字段名按 FieldMapping 配置重命名为目标字段名。
 *
 * <p>未在映射表中配置的源字段将被忽略（不会出现在输出记录中）。
 * target 字段名需与本地动态表列名一致（由 MetadataConfig 驱动的预设列）。</p>
 */
public class FieldMappingService {

    /**
     * 对数据记录列表应用字段映射，将源字段名重命名为目标字段名。
     *
     * @param records      DataProvider 返回的原始记录（字段名为数据源侧定义）
     * @param fieldMapping 字段映射规则列表
     * @return 映射后的记录列表，每条记录只包含映射配置涉及的目标字段
     */
    public List<Map<String, Object>> applyMapping(List<Map<String, Object>> records,
                                                   List<FieldMapping> fieldMapping) {
        List<Map<String, Object>> result = new ArrayList<>(records.size());
        for (Map<String, Object> row : records) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            for (FieldMapping fm : fieldMapping) {
                if (row.containsKey(fm.getSource())) {
                    mapped.put(fm.getTarget(), row.get(fm.getSource()));
                }
            }
            result.add(mapped);
        }
        return result;
    }
}
