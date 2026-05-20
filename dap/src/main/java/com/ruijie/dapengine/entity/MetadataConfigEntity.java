package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_metadata_config 表持久化实体。
 *
 * <p>所有字段名遵循 MyBatis-Plus 默认 camelCase → snake_case 列映射规则。</p>
 *
 * <p>{@code isDelete} 和 {@code required} 使用 {@code Integer} 类型，
 * 与数据库 TINYINT 类型对应，并在 Repository 层按需转换为 boolean。</p>
 *
 * <p>注意：{@code system}（系统字段标志）不是数据库列，由
 * {@code MetadataConfigService} 在内存中根据字段名是否属于系统保留集合动态设置，
 * 因此本实体中不含该字段。</p>
 */
@Data
@TableName("dap_sys_metadata_config")
public class MetadataConfigEntity {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private String tenantId;

    /** 应用标识 */
    private String appCode;

    /** 所属 Subject 的主键 ID（外键） */
    private Long subjectId;

    /** 所属 Subject 的 code（冗余，避免关联查询） */
    private String subjectCode;

    /** 所属 Subject 的 name（冗余） */
    private String subjectName;

    /** 字段名，格式 ^[a-z][a-z0-9_]{0,127}$ */
    private String fieldName;

    /**
     * 字段类型，与 {@link com.ruijie.dapengine.common.enums.FieldType} 枚举值对应：
     * STRING / STRING_LONG / TEXT / INT / DECIMAL / DATE / DATETIME / ENUM
     */
    private String fieldType;

    /** 字符串/枚举字段最大长度；0 或 null 表示按字段类型默认值 */
    private Integer maxLength;

    /** 字段显示标签，最长 128 字符 */
    private String fieldLabel;

    /** 是否必填：1=必填，0=非必填 */
    private Integer required;

    /** ENUM 类型字段关联的字典编码 */
    private String dictCode;

    /** 字段排序权重，系统字段为负数（-100/-99/-98），自定义字段从 10 起步 */
    private Integer sortOrder;

    /** 逻辑删除标志：0=有效，1=废弃 */
    private Integer isDelete;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 创建人 ID */
    private String createdBy;

    /** 最后更新人 ID */
    private String updatedBy;
}
