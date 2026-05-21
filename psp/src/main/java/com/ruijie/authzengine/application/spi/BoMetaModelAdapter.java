package com.ruijie.authzengine.application.spi;

import java.util.List;
import java.util.Map;

/**
 * 业务对象元模型适配器，由宿主业务应用提供具体 Bean 实现。
 *
 * <p><b>Hook 执行状态约定（对应 HookExecutionStatus）</b>：
 * <ul>
 *   <li><b>SUCCESS</b>：{@link #fetchInstanceAttributes} 或 {@link #translateToPhysicalSql} 正常返回非空结果</li>
 *   <li><b>EMPTY_RESULT</b>：{@link #fetchInstanceAttributes} 返回 null 或空 Map；
 *       {@link #translateToPhysicalSql} 抛出 {@link UnsupportedOperationException} 或返回空字符串</li>
 *   <li><b>ERROR</b>：方法抛出 {@link com.ruijie.authzengine.shared.exception.AuthzIntegrationException}
 *       或其他非受检异常，将触发 INDETERMINATE/HOOK_ERROR 决策</li>
 * </ul>
 *
 * <p><b>SQL 安全边界约定</b>（针对 {@link #translateToPhysicalSql}）：
 * <ul>
 *   <li>只允许返回 WHERE 子句片段，禁止返回完整 SQL 语句（SELECT/UPDATE/DELETE/INSERT）</li>
 *   <li>禁止在返回值中包含分号（防止 SQL 注入与多语句执行）</li>
 *   <li>违反上述规则时必须抛出 {@link com.ruijie.authzengine.shared.exception.AuthzIntegrationException}</li>
 * </ul>
 *
 * <p>运行时调用期间，{@link BoMetaModelRuntimeContextHolder#getCurrent()} 返回当前请求完整上下文，
 * 包含 tenantId、appCode、boCode、resolver 等字段，可在实现类中安全读取。
 */
public interface BoMetaModelAdapter {

    /**
     * 根据实例标识加载业务对象属性，供 PIP 注入运行时上下文。
     *
     * <p>返回 null 或空 Map 时，PIP 将记录 EMPTY_RESULT，不影响鉴权结果，
     * 但也不向 AuthzContext 注入任何 BO 属性。
     *
     * @param instanceId 业务对象实例标识
     * @param schemaJson 业务对象 schemaJson 协议
     * @param requestContext 原始鉴权上下文
     * @return 业务对象属性键值对，null 或空 Map 均视为无属性
     */
    Map<String, Object> fetchInstanceAttributes(
        String instanceId,
        String schemaJson,
        Map<String, Object> requestContext
    );

    /**
     * 将语义条件翻译为宿主侧物理过滤片段（WHERE 子句）。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}，表示当前 Hook 不支持 SQL 翻译；
     * 框架会将此情况视为 EMPTY_RESULT 并返回 CONTRACT_ONLY 占位响应。
     *
     * <p>实现类必须遵守 SQL 安全边界约定（见类级注释），违规时抛出
     * {@link com.ruijie.authzengine.shared.exception.AuthzIntegrationException}。
     *
     * @param semanticCondition 语义条件（由框架传入，格式由 schemaJson.sqlTranslation 定义）
     * @param schemaJson 元模型 schema 协议内容
     * @return 物理 WHERE 子句片段，null 或空字符串视为无翻译
     */
    default String translateToPhysicalSql(String semanticCondition, String schemaJson) {
        throw new UnsupportedOperationException("当前 Hook 未实现 SQL 翻译能力");
    }

    /**
     * 采集 BO 的表/列元数据，供治理页展示结构化字段清单供管理员勾选。
     *
     * <p>适用于 Shadow Mode 下宿主系统提供元数据采集能力的场景。
     * 返回 {@code null} 表示宿主未实现该能力，框架将降级为手工录入模式。
     *
     * <p><b>约束</b>：
     * <ul>
     *   <li>返回值仅作为候选输入，<b>不能直接落库</b>，必须经过管理员治理确认。</li>
     *   <li>应支持按 BO / 表名批量查询，避免页面逐字段 N+1 拉取。</li>
     *   <li>宿主未接入此能力时直接返回 {@code null}，不得抛出异常；
     *       若采集过程出现业务错误，才允许抛出异常（框架会向前端返回错误提示）。</li>
     * </ul>
     *
     * @param boCode   BO 编码
     * @param tableName 物理表名；若 BO 尚未绑定表名则可为空，由宿主自行处理
     * @param hints    调用者传入的额外上下文提示（如租户、应用、BO 版本等），宿主可选用
     * @return 结构化列信息列表；null 表示宿主未实现，空列表表示采集结果为空
     */
    default List<BoSchemaColumnInfo> fetchBoSchema(String boCode, String tableName, Map<String, Object> hints) {
        return null;
    }
}
