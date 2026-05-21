package com.ruijie.authzengine.application.spi;

/**
 * 权限模型编码枚举，覆盖主体模型（SUB_*）与资源模型（RES_*）。
 *
 * <p>枚举值与 {@code authz_meta_model} 表的 {@code model_code} 字段一一对应。
 */
public enum ModelCode {

    /** 用户主体 */
    SUB_USER("SUB_USER"),

    /** 角色主体 */
    SUB_ROLE("SUB_ROLE"),

    /** 组织主体 */
    SUB_ORG("SUB_ORG"),

    /** 岗位主体 */
    SUB_POSITION("SUB_POSITION"),

    /** 用户组主体 */
    SUB_GROUP("SUB_GROUP"),

    /** 菜单资源 */
    RES_UI_MENU("RES_UI_MENU"),

    /** 页面资源 */
    RES_UI_PAGE("RES_UI_PAGE"),

    /** 组件资源 */
    RES_UI_COMPONENT("RES_UI_COMPONENT"),

    /** API 资源 */
    RES_API("RES_API"),

    /** 业务对象资源 */
    RES_DATA_BO("RES_DATA_BO");

    private final String code;

    ModelCode(String code) {
        this.code = code;
    }

    /**
     * 获取模型编码字符串值。
     */
    public String getCode() {
        return code;
    }

    /**
     * 根据字符串编码查找对应枚举值。
     *
     * @param code 模型编码字符串
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static ModelCode fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("模型编码不能为空");
        }
        for (ModelCode mc : values()) {
            if (mc.code.equals(code)) {
                return mc;
            }
        }
        throw new IllegalArgumentException("未知的模型编码: " + code);
    }
}
