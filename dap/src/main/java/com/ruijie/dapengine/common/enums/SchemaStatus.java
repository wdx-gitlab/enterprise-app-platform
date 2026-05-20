package com.ruijie.dapengine.common.enums;

/**
 * 主数据类型的 Schema 状态枚举，动态计算，不持久化存储。
 * <ul>
 *   <li>APPLIED：动态表存在且所有有效字段均有对应列</li>
 *   <li>PENDING：动态表不存在，或存在缺列/待扩容列</li>
 * </ul>
 */
public enum SchemaStatus {
    /** 动态表存在，且所有 {@code is_delete=0} 的字段均已在动态表中建列。 */
    APPLIED,
    /** 动态表不存在，或存在缺列 / 待扩容列（需执行 DDL 同步）。 */
    PENDING
}
