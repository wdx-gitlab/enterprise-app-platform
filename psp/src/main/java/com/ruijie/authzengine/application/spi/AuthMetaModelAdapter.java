package com.ruijie.authzengine.application.spi;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 主体元模型适配器，由宿主业务应用提供具体 Bean 实现。
 *
 * <p>本接口承载三类职责：
 * <ul>
 *   <li><b>鉴权上下文补充（PIP 阶段）</b>：{@link #fetchInstanceAttributes} 用于 Shadow Mode 下为 PDP 补全主体属性。
 *       主体关联统一来自引擎库 {@code authz_subject_relation}，不再由 Hook 或宿主业务表提供。</li>
 *   <li><b>同类型治理数据查询/写入（PAP 阶段）</b>：{@code pageItems}/{@code getItem}/{@code createItem}/{@code updateItem}/{@code deleteItem}
 *       等 default 方法，用于 Shadow Mode 下治理页面从宿主系统获取主体与资源列表。
 *       宿主可按需 override；未 override 的方法返回 null，服务层将提示"适配器未实现该操作"。</li>
 *   <li><b>历史扩展点（兼容保留）</b>：{@link #pageAssociatedItems} 仍保留在 SPI 中，
 *       但当前主体关联查询已统一收口到引擎库 {@code authz_subject_relation}，不再依赖宿主适配器查询关系。</li>
 * </ul>
 *
 * <p><b>路由规则</b>：服务层根据 {@code authz_meta_model} 表中的 {@code resolver} 配置决定调用路径：
 * <ul>
 *   <li>{@code resolver} 有效（非空且非 noopHook）→ 通过 Bean 路由调用本适配器的对应方法</li>
 *   <li>{@code resolver} 为空或 noopHook → 走引擎内置 Repository 实现</li>
 * </ul>
 */
public interface AuthMetaModelAdapter {

    // ────────── 鉴权上下文补充（PIP 阶段） ──────────

    /**
    * 根据实例标识加载实例属性，供 PIP 注入运行时上下文。
     *
     * @param modelCode      模型编码
     * @param instanceId     实例标识（主体 ID 或资源 ID）
     * @param requestContext 原始鉴权上下文
     * @return Hook 结果，仅用于补充主体属性
     */
    SubjectHookResult fetchInstanceAttributes(
        ModelCode modelCode,
        String instanceId,
        Map<String, Object> requestContext
    );

    // ────────── 治理数据 CRUD（Shadow Mode 治理页面） ──────────

    /**
     * 查询单条数据详情。返回 null 表示宿主未实现该操作。
     *
     * @param modelCode 模型编码
     * @param itemId    数据标识
     * @return 数据项；null 表示未实现
     */
    default DataItem getItem(ModelCode modelCode, String itemId) {
        return null;
    }

    /**
     * 分页查询指定模型下的数据列表（Shadow Mode 治理页面调用）。
     * <p>返回 null 表示宿主未实现该操作，服务层将提示"适配器未实现"。
     *
     * @param modelCode   模型编码
     * @param queryParams 查询参数（如 keyword、status 等）
     * @param pageNo      页码
     * @param pageSize    每页大小
     * @return 分页结果；null 表示未实现
     */
    default PageResult<DataItem> pageItems(
        ModelCode modelCode, Map<String, String> queryParams, int pageNo, int pageSize) {
        return null;
    }

    /**
     * 批量查询指定编码集合对应的数据项。
     *
     * <p>用于运行时一次加载多条宿主元数据，避免在 user-context 或派生资源装配时逐条 N+1 查询。
     * 返回 null 表示宿主未实现该批量查询操作。
     *
     * @param modelCode 模型编码
     * @param itemCodes 数据编码集合
     * @return 数据项列表；null 表示未实现
     */
    default List<DataItem> batchResolveItems(ModelCode modelCode, List<String> itemCodes) {
        return null;
    }

    /**
     * 创建数据（预留）。返回 null 表示宿主未实现该操作。
     *
     * @param modelCode 模型编码
     * @param item      数据项
     * @return 创建结果；null 表示未实现
     */
    default DataItem createItem(ModelCode modelCode, DataItem item) {
        return null;
    }

    /**
     * 更新数据（预留）。返回 null 表示宿主未实现该操作。
     *
     * @param modelCode 模型编码
     * @param itemId    数据标识
     * @param item      数据项
     * @return 更新结果；null 表示未实现
     */
    default DataItem updateItem(ModelCode modelCode, String itemId, DataItem item) {
        return null;
    }

    /**
     * 删除数据（预留）。返回 false 表示宿主未实现该操作。
     *
     * @param modelCode 模型编码
     * @param itemId    数据标识
     * @return 是否成功；false 表示未实现
     */
    default boolean deleteItem(ModelCode modelCode, String itemId) {
        return false;
    }

    // ────────── 跨类型关联查询（PAP 管理端扩展） ──────────

    /**
     * 分页查询与指定实例存在关联关系的另一类型实例列表（Shadow Mode 治理页面调用）。
     *
     * <p><b>设计意图</b>：解决 PAP 管理端"反向/跨类型关联查询"的场景，例如：
     * <ul>
     *   <li>给定角色 R，查询拥有该角色的用户列表：
     *       {@code pageAssociatedItems(SUB_ROLE, roleId, SUB_USER, ...)}</li>
     *   <li>给定组织 O，查询该组织下的用户列表：
     *       {@code pageAssociatedItems(SUB_ORG, orgId, SUB_USER, ...)}</li>
     * </ul>
     *
     * <p><b>与 pageItems 的区别</b>：{@code pageItems} 是同类型实例的列表查询，语义是"列出所有 X"；
     * 本方法是跨类型关联查询，语义是"给定 A，列出与之关联的所有 B"。两者不可混用。
     *
     * <p><b>当前口径</b>：主体关联统一由引擎库 {@code authz_subject_relation} 维护。
    * 该方法仅作为历史扩展点保留，不再作为主体关系的正式数据源。
     *
     * <p>返回 null 表示宿主未实现该操作，服务层将提示"适配器未实现该关联查询"。
     *
     * @param sourceModel  来源模型编码（如 {@code SUB_ROLE}）
     * @param sourceId     来源实例标识（如 roleId）
     * @param targetModel  目标模型编码（如 {@code SUB_USER}）
     * @param queryParams  附加查询参数（如 keyword）
     * @param pageNo       页码（从 1 开始）
     * @param pageSize     每页条数
     * @return 目标类型的分页结果；null 表示宿主未实现
     */
    default PageResult<DataItem> pageAssociatedItems(
        ModelCode sourceModel, String sourceId,
        ModelCode targetModel,
        Map<String, String> queryParams,
        int pageNo, int pageSize) {
        return null;
    }
}

