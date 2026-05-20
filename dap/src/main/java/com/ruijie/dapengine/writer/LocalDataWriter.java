package com.ruijie.dapengine.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.util.SqlNameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本地动态主数据表写入器。
 *
 * <p>提供两种写入模式：
 * <ul>
 *   <li>{@link #upsert} — DELTA 增量写入（ON DUPLICATE KEY UPDATE，不覆盖 code/created_by/created_at）</li>
 *   <li>{@link #fullRefresh} — FULL_REFRESH 全量刷新（CREATE tmp → INSERT → RENAME TABLE）</li>
 * </ul>
 * 表名和列名均经过 {@link SqlNameValidator} 校验，防止 SQL 注入。</p>
 */
public class LocalDataWriter {

    private static final Logger log = LoggerFactory.getLogger(LocalDataWriter.class);

    /** 每批写入条数 */
    static final int BATCH_SIZE = 500;

    /**
     * UPSERT 时 UPDATE 子句排除的系统列（code 作为唯一键，created_by/created_at 不覆盖）。
     */
    private static final Set<String> EXCLUDE_FROM_UPDATE = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("code", "created_by", "created_at",
                    "id", "tenant_id", "app_code")));

    private final DapEngineJdbcTemplate dapJdbc;
    private final String tenantId;
    private final String appCode;
    private final ObjectMapper objectMapper;

    public LocalDataWriter(DapEngineJdbcTemplate dapJdbc, String tenantId, String appCode) {
        this(dapJdbc, tenantId, appCode, new ObjectMapper());
    }

    public LocalDataWriter(DapEngineJdbcTemplate dapJdbc, String tenantId, String appCode,
                           ObjectMapper objectMapper) {
        this.dapJdbc = dapJdbc;
        this.tenantId = tenantId;
        this.appCode = appCode;
        this.objectMapper = objectMapper;
    }

    /**
     * DELTA 增量写入：INSERT … ON DUPLICATE KEY UPDATE（code 列为唯一键）。
     *
     * <p>records 中的 key 须与表列名一致（经 FieldMappingService 映射后）。
     * 系统列由本方法自动补充（tenant_id, app_code, dap_version, dap_sync_time, updated_by）。</p>
     *
     * @param subjectCode 主题编码（用于确定动态表名 dap_{subjectCode}）
     * @param records     已映射的记录列表
     * @param operator    操作人（写入 updated_by，INSERT 时同时写入 created_by）
     * @return 累计写入/更新条数
     */
    public int upsert(String subjectCode, List<Map<String, Object>> records, String operator) {
        String tableBase = "dap_" + subjectCode.toLowerCase();
        SqlNameValidator.validate(tableBase);

        if (records == null || records.isEmpty()) {
            return 0;
        }

        // 从第一条记录推导业务列名（自定义字段 + code）
        Map<String, Object> first = records.get(0);
        List<String> busColNames = new ArrayList<>(first.keySet());
        // 校验 code 列存在（code 是动态表唯一键，必须来自字段映射配置）
        validateCodeColumn(subjectCode, busColNames);
        // 验证列名合法性
        for (String col : busColNames) {
            SqlNameValidator.validate(col);
        }

        // 全量列名 = 业务列 + 系统列（非业务列）
        List<String> allCols = buildAllColumns(busColNames);

        String insertSql = buildInsertSql(tableBase, allCols);
        String updateSql = buildUpdateClause(allCols);
        String fullSql = insertSql + " ON DUPLICATE KEY UPDATE " + updateSql;

        int total = 0;
        JdbcTemplate jdbc = dapJdbc.getJdbcTemplate();
        List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);

        for (Map<String, Object> row : records) {
            batch.add(row);
            if (batch.size() >= BATCH_SIZE) {
                total += executeBatch(jdbc, fullSql, allCols, batch, operator);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            total += executeBatch(jdbc, fullSql, allCols, batch, operator);
        }
        return total;
    }

    /**
     * 按编码逻辑删除单条记录。
     */
    public int softDeleteByCode(String subjectCode, String code, String operator) {
        String tableBase = "dap_" + subjectCode.toLowerCase();
        SqlNameValidator.validate(tableBase);
        String sql = "UPDATE `" + tableBase + "` SET is_delete = 1, updated_at = ?, updated_by = ?, dap_version = ?, dap_sync_time = ?"
                + " WHERE tenant_id = ? AND app_code = ? AND `code` = ? AND is_delete = 0";
        LocalDateTime now = LocalDateTime.now();
        return dapJdbc.getJdbcTemplate().update(sql,
                now,
                operator != null ? operator : "",
                System.currentTimeMillis(),
                now,
                tenantId,
                appCode,
                code);
    }

    /**
     * 按编码批量逻辑删除。
     */
    public int batchSoftDeleteByCodes(String subjectCode, List<String> codes, String operator) {
        if (codes == null || codes.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (String code : codes) {
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            affected += softDeleteByCode(subjectCode, code, operator);
        }
        return affected;
    }

    /**
     * FULL_REFRESH 全量刷新：CREATE tmp → INSERT → RENAME TABLE（原子）→ 备份旧表。
     *
     * @param subjectCode 主题编码
     * @param records     全量记录
     * @param operator    操作人
     * @return 备份表名（格式：dap_{subjectCode}_bak_{yyyyMMddHHmmss}）
     */
    public String fullRefresh(String subjectCode, List<Map<String, Object>> records, String operator) {
        String tableBase = "dap_" + subjectCode.toLowerCase();
        SqlNameValidator.validate(tableBase);

        String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tmpTable = tableBase + "_tmp";
        String bakTable = tableBase + "_bak_" + ts;
        JdbcTemplate jdbc = dapJdbc.getJdbcTemplate();

        // 1. 确保无残留 tmp 表
        jdbc.execute("DROP TABLE IF EXISTS `" + tmpTable + "`");

        // 2. CREATE TABLE tmp LIKE 正式表（复制表结构和索引）
        jdbc.execute("CREATE TABLE `" + tmpTable + "` LIKE `" + tableBase + "`");

        try {
            // 3. 批量写入 tmp 表
            if (records != null && !records.isEmpty()) {
                List<String> busColNames = new ArrayList<>(records.get(0).keySet());
                // 校验 code 列存在（code 是动态表唯一键，必须来自字段映射配置）
                validateCodeColumn(subjectCode, busColNames);
                for (String col : busColNames) {
                    SqlNameValidator.validate(col);
                }
                List<String> allCols = buildAllColumns(busColNames);
                String insertSql = buildInsertSql(tmpTable, allCols);

                int total = 0;
                List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);
                for (Map<String, Object> row : records) {
                    batch.add(row);
                    if (batch.size() >= BATCH_SIZE) {
                        total += executeBatch(jdbc, insertSql, allCols, batch, operator);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    total += executeBatch(jdbc, insertSql, allCols, batch, operator);
                }
                log.debug("[DAP Engine] fullRefresh inserted {} rows into tmp table {}", total, tmpTable);
            }

            // 4. RENAME TABLE 原子交换：正式表 → bak，tmp → 正式表
            jdbc.execute("RENAME TABLE `" + tableBase + "` TO `" + bakTable + "`, "
                    + "`" + tmpTable + "` TO `" + tableBase + "`");

            return bakTable;

        } catch (Exception ex) {
            // 清理 tmp 表，保持原表不变
            try {
                jdbc.execute("DROP TABLE IF EXISTS `" + tmpTable + "`");
            } catch (Exception ignored) {
                // best-effort cleanup
            }
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // 流式全量刷新：begin / appendPage / commit / rollback
    // 供 SyncExecutor 在流式翻页模式下分页写入 tmp 表，最后一次性 RENAME，
    // 避免将全量数据积累在内存后再写库（大数据集 OOM 风险）。
    // -------------------------------------------------------------------------

    /**
     * 开始流式全量刷新：清理残留 tmp 表并 CREATE TABLE tmp LIKE 正式表。
     * 必须与 {@link #commitFullRefresh} 或 {@link #rollbackFullRefresh} 配对使用。
     */
    public void beginFullRefresh(String subjectCode) {
        String tableBase = "dap_" + subjectCode.toLowerCase();
        SqlNameValidator.validate(tableBase);
        String tmpTable = tableBase + "_tmp";
        JdbcTemplate jdbc = dapJdbc.getJdbcTemplate();
        jdbc.execute("DROP TABLE IF EXISTS `" + tmpTable + "`");
        jdbc.execute("CREATE TABLE `" + tmpTable + "` LIKE `" + tableBase + "`");
        log.debug("[DAP Engine] beginFullRefresh: created tmp table {}", tmpTable);
    }

    /**
     * 向 tmp 表追加一页数据（流式全量刷新中间步骤）。
     *
     * @return 本页实际写入行数
     */
    public int appendFullRefreshPage(String subjectCode, List<Map<String, Object>> records, String operator) {
        if (records == null || records.isEmpty()) return 0;
        String tableBase = "dap_" + subjectCode.toLowerCase();
        String tmpTable = tableBase + "_tmp";
        List<String> busColNames = new ArrayList<>(records.get(0).keySet());
        validateCodeColumn(subjectCode, busColNames);
        for (String col : busColNames) {
            SqlNameValidator.validate(col);
        }
        List<String> allCols = buildAllColumns(busColNames);
        String insertSql = buildInsertSql(tmpTable, allCols);
        int total = 0;
        JdbcTemplate jdbc = dapJdbc.getJdbcTemplate();
        List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);
        for (Map<String, Object> row : records) {
            batch.add(row);
            if (batch.size() >= BATCH_SIZE) {
                total += executeBatch(jdbc, insertSql, allCols, batch, operator);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            total += executeBatch(jdbc, insertSql, allCols, batch, operator);
        }
        return total;
    }

    /**
     * 提交流式全量刷新：RENAME TABLE 原子交换正式表与 tmp 表。
     *
     * @return 备份表名（格式：dap_{subjectCode}_bak_{yyyyMMddHHmmss}）
     */
    public String commitFullRefresh(String subjectCode) {
        String tableBase = "dap_" + subjectCode.toLowerCase();
        String tmpTable = tableBase + "_tmp";
        String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String bakTable = tableBase + "_bak_" + ts;
        dapJdbc.getJdbcTemplate().execute(
                "RENAME TABLE `" + tableBase + "` TO `" + bakTable + "`, "
                + "`" + tmpTable + "` TO `" + tableBase + "`");
        log.debug("[DAP Engine] commitFullRefresh: {} -> {}, {} -> {}",
                tableBase, bakTable, tmpTable, tableBase);
        return bakTable;
    }

    /**
     * 回滚流式全量刷新：仅清理 tmp 表，正式表保持不变。
     */
    public void rollbackFullRefresh(String subjectCode) {
        String tableBase = "dap_" + subjectCode.toLowerCase();
        String tmpTable = tableBase + "_tmp";
        try {
            dapJdbc.getJdbcTemplate().execute("DROP TABLE IF EXISTS `" + tmpTable + "`");
            log.debug("[DAP Engine] rollbackFullRefresh: dropped tmp table {}", tmpTable);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------------------------

    /**
     * 校验记录中必须包含 "code" 列。
     * code 是动态表的业务唯一键，必须通过字段映射配置显式指定，缺失时提前抛出明确异常。
     */
    private void validateCodeColumn(String subjectCode, List<String> busColNames) {
        if (!busColNames.contains("code")) {
            throw new DapValidationException(
                    "[DAP Engine] subject=" + subjectCode + " 的记录中缺少 'code' 列。"
                    + "请检查同步配置的字段映射，将数据源中的唯一标识字段映射到目标字段 'code'。"
                    + "当前记录字段：" + busColNames);
        }
    }

    /**
     * 合并业务列与系统附加列（系统列追加在业务列后，避免重复）。
     */
    private List<String> buildAllColumns(List<String> busColNames) {
        List<String> all = new ArrayList<>(busColNames);
        // 追加系统列（除 code/id，业务列中应已含 code；id 为 AUTO_INCREMENT 不需显式插入）
        for (String sys : Arrays.asList("tenant_id", "app_code", "dap_version",
                "dap_sync_time", "is_delete", "created_at", "updated_at",
                "created_by", "updated_by")) {
            if (!all.contains(sys)) {
                all.add(sys);
            }
        }
        return all;
    }

    private String buildInsertSql(String table, List<String> cols) {
        StringBuilder sb = new StringBuilder("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append("`").append(cols.get(i)).append("`");
            if (i < cols.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append("?");
            if (i < cols.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildUpdateClause(List<String> allCols) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String col : allCols) {
            if (EXCLUDE_FROM_UPDATE.contains(col)) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append("`").append(col).append("`=VALUES(`").append(col).append("`)");
            first = false;
        }
        return sb.toString();
    }

    /**
     * 将记录字段值转换为 JDBC 可安全写入的类型。
     * JSON 数组/对象（Collection、Map）序列化为 JSON 字符串，避免 JDBC 驱动对其做 Java 对象序列化。
     */
    private Object toJdbcValue(Object value) {
        if (value instanceof Collection || value instanceof Map) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                log.warn("[DAP Engine] Failed to serialize value to JSON, using toString: {}", e.getMessage());
                return value.toString();
            }
        }
        return value;
    }

    private int executeBatch(JdbcTemplate jdbc, String sql, List<String> allCols,
                              List<Map<String, Object>> batch, String operator) {
        LocalDateTime now = LocalDateTime.now();
        int[][] counts = jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            int idx = 1;
            for (String col : allCols) {
                switch (col) {
                    case "tenant_id":
                        ps.setString(idx++, tenantId);
                        break;
                    case "app_code":
                        ps.setString(idx++, appCode);
                        break;
                    case "dap_version":
                        ps.setLong(idx++, System.currentTimeMillis());
                        break;
                    case "dap_sync_time":
                        ps.setObject(idx++, now);
                        break;
                    case "is_delete":
                        ps.setInt(idx++, 0);
                        break;
                    case "created_at":
                        ps.setObject(idx++, now);
                        break;
                    case "updated_at":
                        ps.setObject(idx++, now);
                        break;
                    case "created_by":
                        ps.setString(idx++, operator != null ? operator : "");
                        break;
                    case "updated_by":
                        ps.setString(idx++, operator != null ? operator : "");
                        break;
                    default:
                        Object rawVal = toJdbcValue(row.get(col));
                        ps.setObject(idx++, rawVal != null ? rawVal : "");
                        break;
                }
            }
        });
        int total = 0;
        for (int[] arr : counts) {
            for (int c : arr) {
                total += (c >= 0 ? c : 0);
            }
        }
        return total;
    }
}
