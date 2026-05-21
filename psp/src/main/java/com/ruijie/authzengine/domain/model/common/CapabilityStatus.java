package com.ruijie.authzengine.domain.model.common;

/**
 * 能力交付状态，标识某项鉴权能力当前是否可实际执行。
 * <p>
 * 用于 data-scope 等扩展能力，在 Hook 机制尚未接入或配置不完整时返回 CONTRACT_ONLY，
 * 告知调用方"契约已定义但实际能力尚不可用"。
 * </p>
 */
public enum CapabilityStatus {

    /** 能力已就绪，可实际执行鉴权并返回完整结果。 */
    AVAILABLE,

    /** 仅契约定义可用，实际能力尚未接入（如 Hook 未注册、适配器缺失等）。 */
    CONTRACT_ONLY
}