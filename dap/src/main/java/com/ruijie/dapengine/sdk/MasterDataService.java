package com.ruijie.dapengine.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.DapEngineProperties;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.QuerySpec;
import com.ruijie.dapengine.common.model.SortSpec;
import com.ruijie.dapengine.common.model.TreeNode;
import com.ruijie.dapengine.common.util.SqlNameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主数据查询核心服务（内部实现）。
 *
 * <p>聚合缓存操作、元数据白名单校验与动。SQL 查询（{@code dap_{subject}} 表）。
 * 所。SQL 使用 PreparedStatement 占位符，字段名经 {@link SqlNameValidator} 白名单校验，防止 SQL 注入。/p>
 *
 * <p>表名规则：{@code dap_{subjectCode.toLowerCase()}}，所有查询附。tenant_id + app_code 隔离条件。/p>
 */
public class MasterDataService {

    private static final Set<String> QUERY_SYSTEM_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "id", "tenant_id", "app_code", "code", "name", "parent_code",
            "dap_version", "dap_sync_time", "is_delete", "created_at",
            "updated_at", "created_by", "updated_by"
    ));

    private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);

    private final JdbcTemplate jdbc;
    private final MetadataConfigService metaService;
    private final MasterDataCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tenantId;
    private final String appCode;
    private final int treeFullLoadThreshold;

    public MasterDataService(DapEngineJdbcTemplate dapJdbc,
                              MetadataConfigService metaService,
                              MasterDataCacheService cacheService,
                              DapEngineProperties props) {
        this.jdbc = dapJdbc.getJdbcTemplate();
        this.metaService = metaService;
        this.cacheService = cacheService;
        this.tenantId = props.getTenantId();
        this.appCode = props.getAppCode();
        this.treeFullLoadThreshold = props.getCache().getTreeFullLoadThreshold();
    }

    // -------------------------------------------------------------------------
    // getByCode
    // -------------------------------------------------------------------------

    /**
     * 按编码查询单条主数据，缓存优先（Redis Hash / Caffeine），未命中查 DB 后回写缓存。
     * 记录不存在或已删除时返回 null。
     */
    public Map<String, Object> getByCode(String subject, String code) {
        Map<String, Object> cached = cacheService.get(subject, code);
        if (cached != null) {
            return cached;
        }
        String table = tableOf(subject);
        String sql = "SELECT * FROM `" + table + "` WHERE tenant_id = ? AND app_code = ? AND `code` = ? AND is_delete = 0 LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, tenantId, appCode, code);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> result = rows.get(0);
        cacheService.put(subject, code, result);
        return result;
    }

    /**
     * 强类型版 getByCode，委使用 {@link #getByCode(String, String)} 再通过 ObjectMapper 转换。
     * 返回 null 使用 {@code clazz} 转换不执行。
     */
    public <T> T getByCode(String subject, String code, Class<T> clazz) {
        Map<String, Object> result = getByCode(subject, code);
        if (result == null) {
            return null;
        }
        return convertRecord(result, clazz, "getByCode");
    }

    /**
     * 批量按编码查询。
     */
    public Map<String, Map<String, Object>> batchGetByCodes(String subject, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyMap();
        }
        String table = tableOf(subject);
        List<String> normalizedCodes = new ArrayList<>();
        for (String code : codes) {
            if (code != null && !code.trim().isEmpty() && !normalizedCodes.contains(code)) {
                normalizedCodes.add(code);
            }
        }
        if (normalizedCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM `").append(table)
                .append("` WHERE tenant_id = ? AND app_code = ? AND is_delete = 0 AND `code` IN (");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        params.add(appCode);
        for (int i = 0; i < normalizedCodes.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
            params.add(normalizedCodes.get(i));
        }
        sql.append(")");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String code = safeStr(row.get("code"));
            if (!code.isEmpty()) {
                result.put(code, row);
                cacheService.put(subject, code, row);
            }
        }
        return result;
    }

    /**
     * 判断记录是否存在且未逻辑删除。
     */
    public boolean exists(String subject, String code) {
        String table = tableOf(subject);
        String sql = "SELECT COUNT(*) FROM `" + table + "` WHERE tenant_id = ? AND app_code = ? AND `code` = ? AND is_delete = 0";
        Long count = jdbc.queryForObject(sql, Long.class, tenantId, appCode, code);
        return count != null && count > 0;
    }

    // -------------------------------------------------------------------------
    // query
    // -------------------------------------------------------------------------

    /**
     * 条件查询，conditions 的字段名需通过元数据白名单校验。
     * 校验失败使用 {@link DapValidationException}，不执行 SQL。
     */
    public <T> List<T> query(String subject, Map<String, Object> conditions, Class<T> clazz) {
        QuerySpec spec = new QuerySpec();
        spec.setFilters(conditions != null ? new LinkedHashMap<>(conditions) : new LinkedHashMap<String, Object>());
        return list(subject, spec, clazz);
    }

    /**
     * 通用列表查询。
     */
    public List<Map<String, Object>> list(String subject, QuerySpec spec) {
        BuiltQuery query = buildQuery(subject, spec);
        String sql = query.getSelectClause() + query.getFromWhereClause() + query.getOrderClause();
        return jdbc.queryForList(sql, query.getParams().toArray());
    }

    /**
     * 通用列表查询（强类型版）。
     */
    public <T> List<T> list(String subject, QuerySpec spec, Class<T> clazz) {
        List<Map<String, Object>> rows = list(subject, spec);
        return convertRecords(rows, clazz, "list");
    }

    /**
     * 通用分页查询。
     */
    public PageResult<Map<String, Object>> page(String subject, QuerySpec spec, int page, int size) {
        BuiltQuery query = buildQuery(subject, spec);
        int pageNum = Math.max(1, page);
        int pageSize = Math.max(1, size);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + query.getFromWhereClause(), Long.class, query.getParams().toArray());
        if (total == null || total <= 0) {
            return PageResult.of(0, pageNum, pageSize, Collections.<Map<String, Object>>emptyList());
        }
        List<Object> dataParams = new ArrayList<>(query.getParams());
        dataParams.add((pageNum - 1) * pageSize);
        dataParams.add(pageSize);
        String sql = query.getSelectClause() + query.getFromWhereClause() + query.getOrderClause() + " LIMIT ?, ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, dataParams.toArray());
        return PageResult.of(total, pageNum, pageSize, rows);
    }

    /**
     * 通用分页查询（强类型版）。
     */
    public <T> PageResult<T> page(String subject, QuerySpec spec, int page, int size, Class<T> clazz) {
        PageResult<Map<String, Object>> result = page(subject, spec, page, size);
        return PageResult.of(result.getTotal(), result.getPage(), result.getSize(), convertRecords(result.getList(), clazz, "page"));
    }

    /**
     * 统计符合条件的记录总数。
     */
    public long count(String subject, QuerySpec spec) {
        BuiltQuery query = buildQuery(subject, spec);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + query.getFromWhereClause(), Long.class, query.getParams().toArray());
        return total == null ? 0L : total;
    }

    // -------------------------------------------------------------------------
    // search
    // -------------------------------------------------------------------------

    /**
     * 按 name 字段模糊搜索（LIKE %keyword%），返回分页结果。
     * keyword 为空时返回全量分页数据。
     */
    public PageResult<Map<String, Object>> search(String subject, String keyword, int page, int size) {
        QuerySpec spec = new QuerySpec();
        spec.setKeyword(keyword);
        spec.setKeywordFields(Collections.singletonList("name"));
        return page(subject, spec, page, size);
    }

    /**
     * 按 name 字段模糊搜索（强类型版）。
     */
    public <T> PageResult<T> search(String subject, String keyword, int page, int size, Class<T> clazz) {
        QuerySpec spec = new QuerySpec();
        spec.setKeyword(keyword);
        spec.setKeywordFields(Collections.singletonList("name"));
        return page(subject, spec, page, size, clazz);
    }

    // -------------------------------------------------------------------------
    // getTree
    // -------------------------------------------------------------------------

    /**
     * 树形查询。
     * <ul>
     *   <li>数据。&le; {@code treeFullLoadThreshold}：全量加载后内存建树</li>
     *   <li>数据。&gt; 阈值：MySQL 8+ WITH RECURSIVE CTE 。rootCode 向下。/li>
     *   <li>CTE 执行异常（MySQL < 8 或语法错误）时降级内存建树并输出 WARN</li>
     * </ul>
     *
     * @param subject  主题编码
     * @param rootCode 起始根节点编码（空字符串/null 时从最顶层开始）
     * @return 树节点列表
     */
    public List<TreeNode> getTree(String subject, String rootCode) {
        // 校验 subject 是否包含 parent_code 字段（仅树形主数据支。getTree。
        List<FieldConfigDTO> fields = metaService.getActiveFieldDTOs(subject);
        boolean hasParentCode = fields.stream().anyMatch(f -> "parent_code".equals(f.getFieldName()));
        if (!hasParentCode) {
            throw new DapValidationException(
                "[DAP Engine] subject '" + subject + "' 没有 parent_code 字段，不支持树形查询");
        }

        String table = tableOf(subject);
        String countSql = "SELECT COUNT(*) FROM `" + table + "` WHERE tenant_id = ? AND app_code = ? AND is_delete = 0";
        Long count = jdbc.queryForObject(countSql, Long.class, tenantId, appCode);
        if (count == null) {
            count = 0L;
        }

        List<Map<String, Object>> rows;
        if (count <= treeFullLoadThreshold) {
            rows = loadAll(table);
        } else {
            try {
                rows = loadByCte(table, rootCode);
            } catch (DataAccessException e) {
                Throwable cause = e.getCause();
                if (cause instanceof SQLException &&
                        cause.getMessage() != null && cause.getMessage().contains("syntax")) {
                    log.warn("[DAP Engine] WITH RECURSIVE CTE 不支持（MySQL < 8?），降级全量内存建树: {}", cause.getMessage());
                    rows = loadAll(table);
                } else {
                    throw e;
                }
            }
        }
        return buildTree(rows, rootCode);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<Map<String, Object>> loadAll(String table) {
        String sql = "SELECT * FROM `" + table + "` WHERE tenant_id = ? AND app_code = ? AND is_delete = 0";
        return jdbc.queryForList(sql, tenantId, appCode);
    }

    private List<Map<String, Object>> loadByCte(String table, String rootCode) {
        boolean hasRoot = rootCode != null && !rootCode.isEmpty();
        String anchorCondition = hasRoot
                ? "code = ?"
                : "(parent_code IS NULL OR parent_code = '')";

        String cte = "WITH RECURSIVE dap_tree AS (" +
                "SELECT * FROM `" + table + "` WHERE tenant_id = ? AND app_code = ? AND is_delete = 0 AND " + anchorCondition +
                " UNION ALL " +
                "SELECT c.* FROM `" + table + "` c INNER JOIN dap_tree p ON c.parent_code = p.code " +
                "WHERE c.tenant_id = ? AND c.app_code = ? AND c.is_delete = 0" +
                ") SELECT * FROM dap_tree";

        Object[] params;
        if (hasRoot) {
            params = new Object[]{tenantId, appCode, rootCode, tenantId, appCode};
        } else {
            params = new Object[]{tenantId, appCode, tenantId, appCode};
        }
        return jdbc.queryForList(cte, params);
    }

    private List<TreeNode> buildTree(List<Map<String, Object>> rows, String rootCode) {
        Map<String, TreeNode> byCode = new LinkedHashMap<>();
        Map<String, List<TreeNode>> byParent = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String code = safeStr(row.get("code"));
            String name = safeStr(row.get("name"));
            String parentCode = safeStr(row.get("parent_code"));

            Map<String, Object> extra = new LinkedHashMap<>(row);
            extra.remove("code");
            extra.remove("name");
            extra.remove("parent_code");
            extra.remove("tenant_id");
            extra.remove("app_code");
            extra.remove("is_delete");
            extra.remove("id");
            extra.remove("dap_version");
            extra.remove("dap_sync_time");
            extra.remove("created_at");
            extra.remove("updated_at");
            extra.remove("created_by");
            extra.remove("updated_by");

            TreeNode node = TreeNode.builder()
                    .code(code)
                    .name(name)
                    .parentCode(parentCode.isEmpty() ? null : parentCode)
                    .extra(extra)
                    .build();

            byCode.put(code, node);
            String parentKey = parentCode.isEmpty() ? "" : parentCode;
            byParent.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(node);
        }

        // Attach children
        for (TreeNode node : byCode.values()) {
            List<TreeNode> children = byParent.get(node.getCode());
            if (children != null) {
                node.setChildren(children);
            }
        }

        boolean hasRoot = rootCode != null && !rootCode.isEmpty();
        if (hasRoot) {
            TreeNode root = byCode.get(rootCode);
            return root != null ? Collections.singletonList(root) : Collections.emptyList();
        }
        // Return top-level nodes (no parent)
        List<TreeNode> roots = byParent.getOrDefault("", Collections.emptyList());
        return new ArrayList<>(roots);
    }

    private BuiltQuery buildQuery(String subject, QuerySpec spec) {
        QuerySpec actualSpec = spec != null ? spec : new QuerySpec();
        String table = tableOf(subject);
        String selectClause = buildSelectClause(subject, actualSpec.getSelectFields());
        StringBuilder fromWhere = new StringBuilder(" FROM `").append(table).append("` WHERE tenant_id = ? AND app_code = ? AND is_delete = 0");
        List<Object> params = new ArrayList<>(Arrays.asList(tenantId, appCode));

        Map<String, Object> filters = actualSpec.getFilters() != null ? actualSpec.getFilters() : Collections.<String, Object>emptyMap();
        validateFieldNames(subject, filters.keySet());
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            SqlNameValidator.validate(entry.getKey());
            fromWhere.append(" AND `").append(entry.getKey()).append("` = ?");
            params.add(entry.getValue());
        }

        if (actualSpec.getKeyword() != null && !actualSpec.getKeyword().trim().isEmpty()) {
            List<String> keywordFields = resolveKeywordFields(subject, actualSpec.getKeywordFields());
            String like = "%" + actualSpec.getKeyword().trim() + "%";
            fromWhere.append(" AND (");
            for (int i = 0; i < keywordFields.size(); i++) {
                if (i > 0) {
                    fromWhere.append(" OR ");
                }
                fromWhere.append("`").append(keywordFields.get(i)).append("` LIKE ?");
                params.add(like);
            }
            fromWhere.append(")");
        }

        String orderClause = buildOrderClause(subject, actualSpec.getSorts());
        return new BuiltQuery(selectClause, fromWhere.toString(), orderClause, params);
    }

    private String buildSelectClause(String subject, List<String> selectFields) {
        if (selectFields == null || selectFields.isEmpty()) {
            return "SELECT *";
        }
        Set<String> whitelist = queryWhitelist(subject);
        List<String> normalized = new ArrayList<>();
        for (String field : selectFields) {
            if (field == null || field.trim().isEmpty()) {
                continue;
            }
            if (!whitelist.contains(field)) {
                throw new DapValidationException("[DAP Engine] 返回字段 '" + field + "' 不在 subject='" + subject + "' 的可用字段集合中");
            }
            SqlNameValidator.validate(field);
            if (!normalized.contains(field)) {
                normalized.add(field);
            }
        }
        if (normalized.isEmpty()) {
            return "SELECT *";
        }
        StringBuilder select = new StringBuilder("SELECT ");
        for (int i = 0; i < normalized.size(); i++) {
            if (i > 0) {
                select.append(", ");
            }
            select.append("`").append(normalized.get(i)).append("`");
        }
        return select.toString();
    }

    private List<String> resolveKeywordFields(String subject, List<String> keywordFields) {
        List<String> actual = (keywordFields == null || keywordFields.isEmpty())
                ? Collections.singletonList("name")
                : keywordFields;
        validateFieldNames(subject, new LinkedHashSet<>(actual));
        return actual;
    }

    private String buildOrderClause(String subject, List<SortSpec> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return " ORDER BY `updated_at` DESC, `id` DESC";
        }
        Set<String> whitelist = queryWhitelist(subject);
        StringBuilder orderBy = new StringBuilder(" ORDER BY ");
        boolean first = true;
        for (SortSpec sort : sorts) {
            if (sort == null || sort.getField() == null || sort.getField().trim().isEmpty()) {
                continue;
            }
            String field = sort.getField().trim();
            if (!whitelist.contains(field)) {
                throw new DapValidationException("[DAP Engine] 排序字段 '" + field + "' 不在 subject='" + subject + "' 的可用字段集合中");
            }
            SqlNameValidator.validate(field);
            String direction = sort.getDirection() == null ? "ASC" : sort.getDirection().trim().toUpperCase(Locale.ROOT);
            if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
                throw new DapValidationException("[DAP Engine] 排序方向仅支持 ASC / DESC，实际: " + sort.getDirection());
            }
            if (!first) {
                orderBy.append(", ");
            }
            orderBy.append("`").append(field).append("` ").append(direction);
            first = false;
        }
        return first ? " ORDER BY `updated_at` DESC, `id` DESC" : orderBy.toString();
    }

    private Set<String> queryWhitelist(String subject) {
        List<FieldConfigDTO> active = metaService.getActiveFieldDTOs(subject);
        Set<String> whitelist = active.stream()
                .map(FieldConfigDTO::getFieldName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        whitelist.addAll(QUERY_SYSTEM_FIELDS);
        return whitelist;
    }

    private void validateFieldNames(String subject, Set<String> fieldNames) {
        List<FieldConfigDTO> active = metaService.getActiveFieldDTOs(subject);
        Set<String> whitelist = active.stream()
                .map(FieldConfigDTO::getFieldName)
                .collect(Collectors.toSet());
        // System fields always allowed
        whitelist.addAll(Arrays.asList("code", "name", "parent_code"));

        for (String field : fieldNames) {
            if (!whitelist.contains(field)) {
                throw new DapValidationException(
                        "[DAP Engine] 查询字段 '" + field + "' 不在 subject='" + subject + "' 的元数据白名单中");
            }
        }
    }

    private <T> List<T> convertRecords(List<Map<String, Object>> rows, Class<T> clazz, String operation) {
        if (clazz == null || clazz == Map.class) {
            @SuppressWarnings("unchecked")
            List<T> casted = (List<T>) rows;
            return casted;
        }
        List<T> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(convertRecord(row, clazz, operation));
        }
        return result;
    }

    private <T> T convertRecord(Map<String, Object> row, Class<T> clazz, String operation) {
        if (clazz == null || clazz == Map.class) {
            @SuppressWarnings("unchecked")
            T casted = (T) row;
            return casted;
        }
        try {
            return objectMapper.convertValue(row, clazz);
        } catch (IllegalArgumentException ex) {
            throw new DapValidationException("[DAP Engine] " + operation + " 结果转换失败: " + ex.getMessage());
        }
    }

    private static String tableOf(String subjectCode) {
        String name = "dap_" + subjectCode.toLowerCase();
        SqlNameValidator.validate(name);
        return name;
    }

    private static String safeStr(Object val) {
        return val != null ? val.toString() : "";
    }

    private static class BuiltQuery {
        private final String selectClause;
        private final String fromWhereClause;
        private final String orderClause;
        private final List<Object> params;

        private BuiltQuery(String selectClause, String fromWhereClause, String orderClause, List<Object> params) {
            this.selectClause = selectClause;
            this.fromWhereClause = fromWhereClause;
            this.orderClause = orderClause;
            this.params = params;
        }

        private String getSelectClause() {
            return selectClause;
        }

        private String getFromWhereClause() {
            return fromWhereClause;
        }

        private String getOrderClause() {
            return orderClause;
        }

        private List<Object> getParams() {
            return params;
        }
    }
}
