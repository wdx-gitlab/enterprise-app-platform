package com.ruijie.dapengine.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态主题通用查询条件。
 */
@Data
@NoArgsConstructor
public class QuerySpec {

    /** 精确过滤条件（字段名 -> 值，AND 连接） */
    private Map<String, Object> filters = new LinkedHashMap<>();

    /** 模糊搜索关键词 */
    private String keyword;

    /** 参与模糊搜索的字段列表；为空时默认按 name 搜索 */
    private List<String> keywordFields = new ArrayList<>();

    /** 返回字段投影；为空时返回全部字段 */
    private List<String> selectFields = new ArrayList<>();

    /** 排序规则列表 */
    private List<SortSpec> sorts = new ArrayList<>();
}

