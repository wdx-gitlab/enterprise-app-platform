package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.QuerySpec;
import com.ruijie.dapengine.common.model.TreeNode;

import java.util.List;
import java.util.Map;

/**
 * 主数据查询门面接口。
 * 业务系统通过 {@link DapEngineService#query()} 获取该接口实例，执行主数据查询操作。
 */
public interface MasterDataQueryService {

    /**
     * 按编码查询单条主数据。缓存优先，未命中查 DB 后回写缓存。
     *
     * @param subject 主题编码（如 "CUSTOMER"。
     * @param code    记录唯一编码
     * @return 字段 Map；记录不存在或已删除时返。null
     */
    Map<String, Object> getByCode(String subject, String code);

    /**
     * 强类型版 getByCode，结果自动映射到指定 DTO 类型。
     *
     * @param subject 主题编码
     * @param code    记录唯一编码
     * @param clazz   目标类型
     * @param <T>     目标类型泛型
     * @return 映射后的对象；不存在时返。null
     */
    <T> T getByCode(String subject, String code, Class<T> clazz);

    /**
     * 批量按编码查询。
     * 返回。key 。code，value 为对应记录；不存在的 code 不出现在结果中。
     */
    Map<String, Map<String, Object>> batchGetByCodes(String subject, List<String> codes);

    /**
     * 判断指定编码记录是否存在且未删除。
     */
    boolean exists(String subject, String code);

    /**
     * 条件查询，conditions 的字段名需。subject 元数据白名单内，否则。DapValidationException。
     *
     * @param subject    主题编码
     * @param conditions 查询条件（字段名 。值），AND 连接
     * @param clazz      目标类型（传 null 。Map.class 时返。List&lt;Map&gt;。
     * @param <T>        目标类型泛型
     * @return 匹配记录列表
     */
    <T> List<T> query(String subject, Map<String, Object> conditions, Class<T> clazz);

    /**
     * 通用列表查询，支。filters / keyword / projection / sort。
     */
    List<Map<String, Object>> list(String subject, QuerySpec spec);

    /**
     * 通用列表查询（强类型版）。
     */
    <T> List<T> list(String subject, QuerySpec spec, Class<T> clazz);

    /**
     * 通用分页查询，支。QuerySpec。
     */
    PageResult<Map<String, Object>> page(String subject, QuerySpec spec, int page, int size);

    /**
     * 通用分页查询（强类型版）。
     */
    <T> PageResult<T> page(String subject, QuerySpec spec, int page, int size, Class<T> clazz);

    /**
     * 统计符合条件的记录总数。
     */
    long count(String subject, QuerySpec spec);

    /**
     * 按 name 字段模糊搜索，分页返回。keyword 为空时返回全量分页）
     *
     * @param subject  主题编码
     * @param keyword  搜索关键。
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @param clazz    目标类型（传 null 。Map.class 时元素类型为 Map。
     * @param <T>      目标类型泛型
     * @return 分页结果
     */
    <T> PageResult<T> search(String subject, String keyword, int page, int size, Class<T> clazz);

    /**
     * 树形查询。数据量 &le; 5000 条时内存建树，否则使。WITH RECURSIVE CTE。
     *
     * @param subject  主题编码
     * @param rootCode 起始根节点编码（null/空时从最顶层开始）
     * @return 树节点列表
     */
    List<TreeNode> getTree(String subject, String rootCode);
}
