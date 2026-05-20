package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 树形查询结果节点。
 * {@code children} 默认为空列表，不为 null。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {

    private String code;
    private String name;
    private String parentCode;

    @Builder.Default
    private List<TreeNode> children = new ArrayList<>();

    /** 除 code/name/parentCode 之外的其他动态字段 */
    private Map<String, Object> extra;
}
