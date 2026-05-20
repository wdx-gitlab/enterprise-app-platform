package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_subject 表持久化实体。
 *
 * <p>所有字段名遵循 MyBatis-Plus 默认 camelCase → snake_case 列映射规则，
 * 无需额外 {@code @TableField} 注解（id 除外）。</p>
 *
 * <p>注意：{@code isTree} 和 {@code isDelete} 使用 {@code Integer} 类型而非
 * {@code boolean}，以避免 Lombok {@code @Data} 在 boolean 字段生成 {@code isXxx()}
 * 前缀 getter 导致 MyBatis-Plus 列名映射偏移（误识别为 {@code tree} / {@code delete}）。</p>
 */
@Data
@TableName("dap_sys_subject")
public class SubjectEntity {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户 ID，多租户隔离键 */
    private String tenantId;

    /** 应用标识，同租户多应用隔离键 */
    private String appCode;

    /** Subject 唯一编码，格式 ^[A-Z][A-Z0-9_]{1,29}$ */
    private String code;

    /** Subject 显示名称，最长 128 字符 */
    private String name;

    /** Subject 可选描述，最长 512 字符 */
    private String description;

    /** 是否树形主数据：1=树形，0=平铺 */
    private Integer isTree;

    /** 是否内置主题：1=内置不可删除，0=普通 */
    private Integer isBuiltIn;

    /** 状态：1=启用，0=停用 */
    private Integer status;

    /** 逻辑删除标志：0=正常，1=已删除 */
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
