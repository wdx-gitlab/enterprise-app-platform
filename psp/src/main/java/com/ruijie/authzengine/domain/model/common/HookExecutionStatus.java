package com.ruijie.authzengine.domain.model.common;

/**
 * Hook 执行状态枚举，用于审计日志的 hook_status 字段。
 *
 * <ul>
 *   <li>{@link #SUCCESS}：Hook 成功返回非空结果并已补全上下文</li>
 *   <li>{@link #EMPTY_RESULT}：Hook 返回空结果（未补全，但不视为错误）</li>
 *   <li>{@link #REJECTED}：Hook 返回的属性中包含保留字段或安全校验失败，结果被丢弃</li>
 *   <li>{@link #ERROR}：Hook 调用过程中抛出异常，导致集成失败</li>
 * </ul>
 */
public enum HookExecutionStatus {

    /** Hook 成功返回非空结果并补全上下文 */
    SUCCESS,

    /** Hook 返回空结果（resolved=false），不视为错误 */
    EMPTY_RESULT,

    /** Hook 返回结果包含保留字段或安全校验失败，结果被拒绝 */
    REJECTED,

    /** Hook 调用异常，集成失败 */
    ERROR
}
