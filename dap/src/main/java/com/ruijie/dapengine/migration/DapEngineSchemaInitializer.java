package com.ruijie.dapengine.migration;

import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.SchemaChangeResult;
import com.ruijie.dapengine.common.util.FieldSchemaHelper;
import com.ruijie.dapengine.common.util.SqlNameValidator;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.sync.SyncScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态 Schema 引擎核心类，负责根据元数据配置在平台库中建表、补列、类型扩容。
 * <p>
 * 实现 {@link ApplicationRunner}，Starter 启动时自动遍历所有有效 Subject 执行一次兜底 apply。
 * </p>
 * <p>
 * DDL 安全约束：仅允许 {@code CREATE TABLE IF NOT EXISTS}、{@code ALTER TABLE ADD COLUMN}、
 * {@code ALTER TABLE ALTER COLUMN}（类型展宽）。禁止生成 {@code DROP TABLE}、{@code DROP COLUMN}、
 * {@code CHANGE COLUMN}。
 * </p>
 */
public class DapEngineSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DapEngineSchemaInitializer.class);

    /** 动态表名前缀 */
    private static final String TABLE_PREFIX = "dap_";

    /**
     * 合法类型展宽路径。key 为当前 FieldType token，value 为允许的目标 token 集合。
     * 仅这些路径的字段变更会生成 ALTER COLUMN DDL；其余由 MetadataConfigService 元数据层拦截。
     */
    private static final Map<String, List<String>> DB_WIDENING_PATHS;
    static {
        Map<String, List<String>> m = new HashMap<>();
        m.put("STRING",      Arrays.asList("STRING_LONG", "TEXT"));
        m.put("STRING_LONG", Arrays.asList("TEXT"));
        m.put("INT",         Arrays.asList("DECIMAL"));
        m.put("DATE",        Arrays.asList("DATETIME"));
        DB_WIDENING_PATHS = Collections.unmodifiableMap(m);
    }

    /**
     * 系统列 DDL 片段列表（按 FR-011 顺序）。
     * 这些列固定出现在每个动态主数据表中，不需要 diff。
     */
    private static final List<String> SYSTEM_COLUMN_DEFS = Arrays.asList(
        "id           BIGINT       PRIMARY KEY AUTO_INCREMENT",
        "tenant_id    VARCHAR(64)  NOT NULL DEFAULT ''",
        "app_code     VARCHAR(64)  NOT NULL DEFAULT ''",
        "code         VARCHAR(128) NOT NULL",
        "name         VARCHAR(128) NOT NULL DEFAULT ''",
        "parent_code  VARCHAR(128) DEFAULT ''",
        "dap_version  BIGINT       NOT NULL DEFAULT 0",
        "dap_sync_time DATETIME",
        "is_delete    TINYINT      NOT NULL DEFAULT 0",
        "created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP",
        "updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
        "created_by   VARCHAR(64)  NOT NULL DEFAULT ''",
        "updated_by   VARCHAR(64)  NOT NULL DEFAULT ''"
    );

    /** 系统列名集合，用于跳过 diff（系统列不参与 ADD COLUMN / MODIFY COLUMN） */
    private static final List<String> SYSTEM_COLUMN_NAMES = Arrays.asList(
        "id", "tenant_id", "app_code", "code", "name", "parent_code",
        "dap_version", "dap_sync_time", "is_delete",
        "created_at", "updated_at", "created_by", "updated_by"
    );

    private final DapEngineJdbcTemplate dapJdbcTemplate;
    private final MetadataConfigService metaService;
    private final SchemaStatusService schemaStatusService;

    /** Phase 4 可选依赖；为 null 时跳过调度器回调 */
    @Autowired(required = false)
    SyncScheduler syncScheduler;

    public DapEngineSchemaInitializer(DapEngineJdbcTemplate dapJdbcTemplate,
                                      MetadataConfigService metaService,
                                      SchemaStatusService schemaStatusService) {
        this.dapJdbcTemplate = dapJdbcTemplate;
        this.metaService = metaService;
        this.schemaStatusService = schemaStatusService;
    }

    // -------------------------------------------------------------------------
    // US4: ApplicationRunner 启动兜底
    // -------------------------------------------------------------------------

    /**
     * Starter 启动时自动遍历所有有效 Subject，依次执行 applySchema 兜底。
     * 单个 Subject 失败时捕获异常并记录日志，不阻断其余 Subject 的处理。
     */
    @Override
    public void run(ApplicationArguments args) {
        List<String> codes = metaService.listSubjectCodes();
        for (String code : codes) {
            try {
                SchemaChangeResult result = applySchema(code);
                if (!result.getExecutedDdl().isEmpty()) {
                    log.info("[DAP Engine] 启动 Schema 兜底: subject={}, executedDdl={}",
                        code, result.getExecutedDdl());
                }
            } catch (Exception e) {
                log.warn("[DAP Engine] 启动 Schema 兜底失败: subject={}, 错误={}",
                    code, e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // US1-US3: applySchema 核心逻辑
    // -------------------------------------------------------------------------

    /**
     * 应用 Schema 变更，返回本次执行的 DDL 语句列表（幂等时为空列表）。
     * <p>
     * 执行流程：校验 subject → 获取有效字段 → 判断表存在性 →
     * 不存在则建表，存在则 diff（ADD COLUMN + MODIFY COLUMN 仅类型展宽）→
     * 可选触发 SyncScheduler。
     * </p>
     *
     * @param subjectCode Subject code（如 CUSTOMER）
     * @return SchemaChangeResult，含 subject、table、executedDdl
     */
    public SchemaChangeResult applySchema(String subjectCode) {
        // 校验 subject 存在且有效
        metaService.validateSubject(subjectCode);

        String table = TABLE_PREFIX + subjectCode.toLowerCase();
        SqlNameValidator.validate(table);

        List<FieldConfigDTO> activeFields = metaService.getActiveFieldDTOs(subjectCode);
        List<String> executedDdl = new ArrayList<>();

        if (!schemaStatusService.tableExists(table)) {
            // US1: 建表
            String ddl = buildCreateTableDdl(table, activeFields);
            dapJdbcTemplate.getJdbcTemplate().execute(ddl);
            executedDdl.add(ddl);
        } else {
            // US2 + US3: diff 补列 / 类型展宽
            Map<String, String> colTypeMap = schemaStatusService.getColumnTypeMap(table);
            if (colTypeMap == null) {
                colTypeMap = Collections.emptyMap();
            }
            Map<String, SchemaStatusService.ColumnDefinition> columnDefinitionMap = schemaStatusService.getColumnDefinitionMap(table);
            if (columnDefinitionMap == null) {
                columnDefinitionMap = Collections.emptyMap();
            }
            Set<String> physicalColumnSet = new HashSet<String>(columnDefinitionMap.keySet());
            for (FieldConfigDTO field : activeFields) {
                String colName = field.getFieldName();
                String normalizedColName = colName == null ? "" : colName.toLowerCase();
                boolean columnExists = physicalColumnSet.contains(normalizedColName)
                    || columnDefinitionMap.containsKey(normalizedColName);
                // 系统列已存在时直接跳过，避免启动兜底重复 ADD COLUMN
                if (SYSTEM_COLUMN_NAMES.contains(normalizedColName) && columnExists) {
                    continue;
                }
                if (!columnExists) {
                    // US2: 新列 ADD COLUMN
                    String ddl = buildAddColumnDdl(table, field);
                    dapJdbcTemplate.getJdbcTemplate().execute(ddl);
                    executedDdl.add(ddl);
                    physicalColumnSet.add(normalizedColName);
                } else {
                    if (!columnDefinitionMap.containsKey(normalizedColName)) {
                        continue;
                    }
                    SchemaStatusService.ColumnDefinition currentColumn = columnDefinitionMap.get(normalizedColName);
                    String currentToken = colTypeMap.get(normalizedColName);
                    if (!schemaStatusService.isColumnCompatible(field, currentColumn)
                            && isAlterRequired(currentToken, currentColumn, field)) {
                        String ddl = buildModifyColumnDdl(table, field);
                        dapJdbcTemplate.getJdbcTemplate().execute(ddl);
                        executedDdl.add(ddl);
                    }
                }
            }
        }

        SchemaStatus finalStatus = schemaStatusService.computeStatus(subjectCode, activeFields);
        if (finalStatus != SchemaStatus.APPLIED) {
            List<String> pendingReasons = schemaStatusService.describePendingReasons(subjectCode, activeFields);
            throw new DapValidationException(
                "[DAP Engine] apply-schema 执行后 subject '" + subjectCode + "' 仍存在未应用 Schema 变更："
                    + (pendingReasons.isEmpty() ? "请检查字段类型或长度配置" : String.join("；", pendingReasons)));
        }

        // US5: apply 成功后若 SyncScheduler 存在则触发重新注册
        if (syncScheduler != null) {
            syncScheduler.reschedule(subjectCode);
        }

        return new SchemaChangeResult(subjectCode, table, executedDdl);
    }

    // -------------------------------------------------------------------------
    // DDL 构建：仅允许安全操作
    // -------------------------------------------------------------------------

    private String buildCreateTableDdl(String table, List<FieldConfigDTO> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(table).append(" (\n");
        // 系统列（固定）
        for (String colDef : SYSTEM_COLUMN_DEFS) {
            sb.append("  ").append(colDef).append(",\n");
        }
        // 自定义字段列（跳过系统列名冲突的字段）
        for (FieldConfigDTO field : fields) {
            String colName = field.getFieldName();
            if (SYSTEM_COLUMN_NAMES.contains(colName)) {
                continue;
            }
            SqlNameValidator.validate(colName);
            sb.append("  ").append(colName).append(" ").append(toDbType(field.getFieldType(), field.getMaxLength())).append(",\n");
        }
        // 移除末尾逗号
        int lastComma = sb.lastIndexOf(",");
        if (lastComma >= 0) {
            sb.deleteCharAt(lastComma);
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildAddColumnDdl(String table, FieldConfigDTO field) {
        SqlNameValidator.validate(field.getFieldName());
        return "ALTER TABLE " + table + " ADD COLUMN " +
               field.getFieldName() + " " + toDbType(field.getFieldType(), field.getMaxLength());
    }

    /**
     * 构建类型展宽 DDL。使用 MySQL {@code MODIFY COLUMN} 语法修改列类型。
     */
    private String buildModifyColumnDdl(String table, FieldConfigDTO field) {
        SqlNameValidator.validate(field.getFieldName());
        return "ALTER TABLE " + table + " MODIFY COLUMN " +
               field.getFieldName() + " " + toDbType(field.getFieldType(), field.getMaxLength());
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    /**
     * 判断从 currentToken 到 desiredToken 是否为合法的类型展宽路径。
     */
    boolean isWideningRequired(String currentToken, String desiredToken) {
        List<String> allowed = DB_WIDENING_PATHS.get(currentToken);
        return allowed != null && allowed.contains(desiredToken);
    }

    boolean isAlterRequired(String currentToken, SchemaStatusService.ColumnDefinition currentColumn, FieldConfigDTO field) {
        if (field == null || field.getFieldType() == null) {
            return false;
        }
        String fieldType = field.getFieldType();
        if (FieldSchemaHelper.isVarcharFamily(fieldType)) {
            Integer desiredLength = FieldSchemaHelper.normalizeMaxLength(fieldType, field.getMaxLength());
            Integer currentLength = currentColumn == null ? null : currentColumn.getMaxLengthAsInteger();
            if (currentLength == null) {
                return isStringFamilyToken(currentToken);
            }
            return desiredLength != null && desiredLength > currentLength;
        }
        if (currentToken == null) {
            return false;
        }
        if ("TEXT".equals(fieldType) || "DECIMAL".equals(fieldType) || "DATETIME".equals(fieldType)) {
            return isWideningRequired(currentToken, fieldType);
        }
        return false;
    }

    private boolean isStringFamilyToken(String token) {
        return "STRING".equals(token) || "STRING_LONG".equals(token) || "ENUM".equals(token);
    }

    /**
     * FieldType 枚举名 → MySQL 列类型字符串（按 spec.md FR-008）。
     */
    String toDbType(String fieldType) {
        return toDbType(fieldType, null);
    }

    String toDbType(String fieldType, Integer maxLength) {
        return FieldSchemaHelper.toDbType(fieldType, maxLength);
    }
}
