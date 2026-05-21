package com.ruijie.authzengine.domain.model.governance.subject;

import java.util.Locale;

/**
 * 主体关系类型归一化工具。
 *
 * <p>兼容治理页历史别名与标准关系类型之间的差异，统一收口到
 * ROLE / ORG / POSITION / GROUP 等标准值，避免同一关系因别名不同导致
 * 前后端展示不一致或重复写入。</p>
 */
public final class SubjectRelationTypeNormalizer {

    private SubjectRelationTypeNormalizer() {
    }

    /**
     * 按关联主体模型将关系类型归一化为标准值。
     *
     * @param relatedSubjectModel 关联主体模型
     * @param relationType 原始关系类型
     * @return 归一化后的关系类型；无法识别时返回原始大写值
     */
    public static String normalize(String relatedSubjectModel, String relationType) {
        String normalizedType = relationType == null ? "" : relationType.trim();
        if (normalizedType.isEmpty()) {
            return defaultRelationType(relatedSubjectModel, null);
        }
        normalizedType = normalizedType.toUpperCase(Locale.ROOT);
        switch (normalizedType) {
            case "HAS_ROLE":
                return "ROLE";
            case "HAS_POSITION":
                return "POSITION";
            case "IN_GROUP":
                return "GROUP";
            case "MEMBER_OF":
                return defaultRelationType(relatedSubjectModel, normalizedType);
            default:
                return normalizedType;
        }
    }

    private static String defaultRelationType(String relatedSubjectModel, String fallback) {
        if ("SUB_ROLE".equals(relatedSubjectModel)) {
            return "ROLE";
        }
        if ("SUB_ORG".equals(relatedSubjectModel)) {
            return "ORG";
        }
        if ("SUB_POSITION".equals(relatedSubjectModel)) {
            return "POSITION";
        }
        if ("SUB_GROUP".equals(relatedSubjectModel)) {
            return "GROUP";
        }
        return fallback;
    }
}