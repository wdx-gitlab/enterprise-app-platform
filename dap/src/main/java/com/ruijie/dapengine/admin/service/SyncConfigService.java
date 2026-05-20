package com.ruijie.dapengine.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.FieldMapping;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SyncConfigDTO;
import com.ruijie.dapengine.common.model.SyncConfigRequest;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.util.AesCipher;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import com.ruijie.dapengine.sync.SyncScheduler;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同步配置核心业务服务。
 *
 * <p>save() 实现 7 步校验+持久化+调度流程：
 * <ol>
 *   <li>校验 Subject 存在且 schemaStatus == APPLIED</li>
 *   <li>校验 providerType 对应必填字段</li>
 *   <li>SCHEDULE 模式时校验 cronExpr 格式</li>
 *   <li>校验 fieldMapping.target 在元数据中存在</li>
 *   <li>序列化 datasourceConfig JSON 并对 password/敏感 Header AES 加密</li>
 *   <li>落库（INSERT-or-UPDATE）</li>
 *   <li>触发 syncScheduler.reschedule（可为 null）</li>
 * </ol>
 * </p>
 */
public class SyncConfigService {

    /** 掩码值，替换敏感字段（password、Authorization 类 Header）的明文内容 */
    private static final String MASK = "****";

    /** HTTP Authorization 类 Header 名称前缀（不区分大小写）  */
    private static final String AUTH_HEADER_PREFIX = "authorization";

    private final SyncConfigRepository syncConfigRepository;
    private final SubjectRepository subjectRepository;
    private final MetadataConfigService metadataConfigService;
    private final SchemaStatusService schemaStatusService;
    private final AesCipher aesCipher;
    private final SyncScheduler syncScheduler;
    private final ObjectMapper objectMapper;

    public SyncConfigService(SyncConfigRepository syncConfigRepository,
                             SubjectRepository subjectRepository,
                             MetadataConfigService metadataConfigService,
                             SchemaStatusService schemaStatusService,
                             AesCipher aesCipher,
                             SyncScheduler syncScheduler,
                             ObjectMapper objectMapper) {
        this.syncConfigRepository = syncConfigRepository;
        this.subjectRepository = subjectRepository;
        this.metadataConfigService = metadataConfigService;
        this.schemaStatusService = schemaStatusService;
        this.aesCipher = aesCipher;
        this.syncScheduler = syncScheduler;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存同步配置（7 步流程）。
     *
     * @param subjectCode Subject code
     * @param request     同步配置请求体（含明文敏感字段）
     * @param operator    操作人 ID
     */
    @Transactional("dapTransactionManager")
    public void save(String subjectCode, SyncConfigRequest request, String operator) {
        // Step 1: 校验 Subject 存在且 schemaStatus == APPLIED
        SubjectDTO subject = subjectRepository.findByCode(subjectCode);
        if (subject == null || subject.getIsDelete() == 1) {
            throw new DapValidationException(
                    "[DAP Engine] Subject not found: " + subjectCode);
        }
        List<FieldConfigDTO> activeFields = metadataConfigService.getActiveFieldDTOs(subjectCode);
        SchemaStatus schemaStatus = schemaStatusService.computeStatus(subjectCode, activeFields);
        if (schemaStatus != SchemaStatus.APPLIED) {
            throw new DapValidationException(
                    "[DAP Engine] Subject schema is not APPLIED for subjectCode=" + subjectCode
                    + "; schemaStatus=" + schemaStatus + ". Please apply schema first.");
        }

        // Step 2: 校验 providerType 对应必填字段
        validateProviderRequiredFields(request);

        // Step 3: SCHEDULE 模式时校验 cronExpr（使用 Spring 5.2.x CronSequenceGenerator）
        if ("SCHEDULE".equals(request.getSyncMode())) {
            if (isEmpty(request.getCronExpr())) {
                throw new DapValidationException(
                        "[DAP Engine] cronExpr is required when syncMode=SCHEDULE.");
            }
            if (!CronSequenceGenerator.isValidExpression(request.getCronExpr())) {
                throw new DapValidationException(
                        "[DAP Engine] Invalid cronExpr: " + request.getCronExpr()
                        + ". Must be a valid Spring cron expression (6 fields including seconds).");
            }
        } else if ("EVENT".equals(request.getSyncMode())) {
            if (!isEmpty(request.getCronExpr())) {
                throw new DapValidationException(
                        "[DAP Engine] cronExpr must be null or empty when syncMode=EVENT.");
            }
        }

        // Step 4: 校验 fieldMapping.target 在元数据中存在（允许系统字段 code/name/parent_code）
        validateFieldMappingTargets(request.getFieldMapping(), activeFields);

        // Step 5: 处理密码掩码还原：若提交掩码 "****"，从现有配置还原已加密密码（避免覆盖）
        SyncDataSourceConfig dsConfig = request.getDatasourceConfig();
        if (dsConfig != null && MASK.equals(dsConfig.getPassword())) {
            SyncConfigEntity existingEntity = syncConfigRepository.findBySubjectCode(subjectCode);
            if (existingEntity != null && !isEmpty(existingEntity.getDatasourceConfig())) {
                try {
                    SyncDataSourceConfig existingDs = objectMapper.readValue(
                            existingEntity.getDatasourceConfig(), SyncDataSourceConfig.class);
                    dsConfig.setPassword(existingDs.getPassword());
                } catch (Exception ignored) {
                    dsConfig.setPassword(null);
                }
            } else {
                dsConfig.setPassword(null);
            }
        }
        encryptSensitiveFields(dsConfig);
        String datasourceConfigJson;
        try {
            datasourceConfigJson = objectMapper.writeValueAsString(dsConfig);
        } catch (Exception e) {
            throw new DapValidationException(
                    "[DAP Engine] Failed to serialize datasourceConfig: " + e.getMessage());
        }

        String fieldMappingJson;
        try {
            fieldMappingJson = objectMapper.writeValueAsString(request.getFieldMapping());
        } catch (Exception e) {
            throw new DapValidationException(
                    "[DAP Engine] Failed to serialize fieldMapping: " + e.getMessage());
        }

        // Step 6: 落库
        SyncConfigEntity entity = new SyncConfigEntity();
        entity.setSubjectId(subject.getId());
        entity.setSubjectCode(subjectCode);
        entity.setSubjectName(subject.getName());
        entity.setSyncMode(request.getSyncMode());
        entity.setProviderType(request.getProviderType());
        entity.setCronExpr(request.getCronExpr());
        entity.setDatasourceConfig(datasourceConfigJson);
        entity.setFieldMapping(fieldMappingJson);
        entity.setSyncAction(request.getSyncAction());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        entity.setIsDelete(0);
        entity.setUpdatedBy(operator == null ? "" : operator);
        entity.setCreatedBy(operator == null ? "" : operator);
        syncConfigRepository.save(entity);

        // Step 7: 触发调度器重新注册（可为 null，Phase 7 实现）
        if (syncScheduler != null) {
            syncScheduler.reschedule(subjectCode);
        }
    }

    /**
     * 查询同步配置，反序列化后对敏感字段掩码处理。
     *
     * @return 脱敏后的配置 DTO；不存在时返回 {@code null}
     */
    public SyncConfigDTO get(String subjectCode) {
        SyncConfigEntity entity = syncConfigRepository.findBySubjectCode(subjectCode);
        if (entity == null) {
            return null;
        }

        SyncConfigDTO dto = new SyncConfigDTO();
        dto.setSubjectCode(entity.getSubjectCode());
        dto.setSubjectName(entity.getSubjectName());
        dto.setSyncMode(entity.getSyncMode());
        dto.setProviderType(entity.getProviderType());
        dto.setCronExpr(entity.getCronExpr());
        dto.setSyncAction(entity.getSyncAction());
        dto.setStatus(entity.getStatus());

        // 反序列化 fieldMapping
        if (!isEmpty(entity.getFieldMapping())) {
            try {
                List<FieldMapping> fieldMapping = objectMapper.readValue(
                        entity.getFieldMapping(), new TypeReference<List<FieldMapping>>() {});
                dto.setFieldMapping(fieldMapping);
            } catch (Exception e) {
                // 日志由调用方决定，此处静默处理
            }
        }

        // 反序列化 datasourceConfig + 解密 + 掩码
        if (!isEmpty(entity.getDatasourceConfig())) {
            try {
                SyncDataSourceConfig dsConfig = objectMapper.readValue(
                        entity.getDatasourceConfig(), SyncDataSourceConfig.class);
                decryptAndMask(dsConfig);
                dto.setDatasourceConfig(dsConfig);
            } catch (Exception e) {
                // 静默处理，返回 null datasourceConfig
            }
        }

        return dto;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void validateProviderRequiredFields(SyncConfigRequest request) {
        String providerType = request.getProviderType();
        SyncDataSourceConfig ds = request.getDatasourceConfig();
        if (ds == null) {
            throw new DapValidationException("[DAP Engine] datasourceConfig is required.");
        }
        if ("HTTP".equals(providerType)) {
            if (isEmpty(ds.getUrl())) {
                throw new DapValidationException(
                        "[DAP Engine] datasourceConfig.url is required for providerType=HTTP.");
            }
            if (isEmpty(ds.getMethod())) {
                throw new DapValidationException(
                        "[DAP Engine] datasourceConfig.method is required for providerType=HTTP.");
            }
        } else if ("DB".equals(providerType)) {
            if (isEmpty(ds.getJdbcUrl())) {
                throw new DapValidationException(
                        "[DAP Engine] datasourceConfig.jdbcUrl is required for providerType=DB.");
            }
            if (isEmpty(ds.getUsername())) {
                throw new DapValidationException(
                        "[DAP Engine] datasourceConfig.username is required for providerType=DB.");
            }
            if (isEmpty(ds.getQuerySql())) {
                throw new DapValidationException(
                        "[DAP Engine] datasourceConfig.querySql is required for providerType=DB.");
            }
            // DB + DELTA 模式时 querySql 必须包含增量占位符 ${lastSyncTime}
            if ("DELTA".equals(request.getSyncAction()) && !ds.getQuerySql().contains("${lastSyncTime}")) {
                throw new DapValidationException(
                        "[DAP Engine] DB+DELTA 模式下 querySql 必须包含增量占位符 ${lastSyncTime}。");
            }
        } else if ("MQ".equals(providerType)) {
            if (isEmpty(ds.getBootstrapServers()) && isEmpty(ds.getNameServer())) {
                throw new DapValidationException(
                        "[DAP Engine] datasourceConfig.bootstrapServers or nameServer is required for providerType=MQ.");
            }
            // MQ 为事件驱动，不支持 SCHEDULE 同步模式
            if ("SCHEDULE".equals(request.getSyncMode())) {
                throw new DapValidationException(
                        "[DAP Engine] providerType=MQ 不支持 syncMode=SCHEDULE，请使用 EVENT 或 MANUAL 模式。");
            }
        } else {
            throw new DapValidationException(
                    "[DAP Engine] Unknown providerType: " + providerType
                    + ". Allowed: HTTP, DB, MQ.");
        }
    }

    private void validateFieldMappingTargets(List<FieldMapping> fieldMapping,
                                             List<FieldConfigDTO> activeFields) {
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            throw new DapValidationException("[DAP Engine] fieldMapping must not be empty.");
        }
        // 允许的 target 集合 = 元数据字段名 + 系统内置字段 code/name/parent_code
        Set<String> allowedTargets = new HashSet<>();
        allowedTargets.add("code");
        allowedTargets.add("name");
        allowedTargets.add("parent_code");
        for (FieldConfigDTO field : activeFields) {
            allowedTargets.add(field.getFieldName().toLowerCase());
        }
        for (FieldMapping mapping : fieldMapping) {
            if (isEmpty(mapping.getSource())) {
                throw new DapValidationException(
                        "[DAP Engine] fieldMapping.source must not be empty.");
            }
            if (isEmpty(mapping.getTarget())) {
                throw new DapValidationException(
                        "[DAP Engine] fieldMapping.target must not be empty.");
            }
            if (!allowedTargets.contains(mapping.getTarget().toLowerCase())) {
                throw new DapValidationException(
                        "[DAP Engine] fieldMapping.target '" + mapping.getTarget()
                        + "' does not exist in subject metadata fields.");
            }
        }
    }

    /**
     * 对 datasourceConfig 中的敏感字段进行 AES 加密（原地修改）：
     * - DB: password
     * - HTTP: headers 中 key 含 "authorization" 的 value
     */
    private void encryptSensitiveFields(SyncDataSourceConfig ds) {
        if (ds == null) {
            return;
        }
        if (!isEmpty(ds.getPassword())) {
            // 已还原的加密密码（源自现有配置）不再重复加密
            if (!aesCipher.isEncrypted(ds.getPassword())) {
                ds.setPassword(aesCipher.encrypt(ds.getPassword()));
            }
        }
        if (ds.getHeaders() != null) {
            Map<String, String> headers = ds.getHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().contains(AUTH_HEADER_PREFIX)
                        && !isEmpty(entry.getValue())) {
                    entry.setValue(aesCipher.encrypt(entry.getValue()));
                }
            }
        }
    }

    /**
     * 解密 datasourceConfig 中的敏感字段，再用掩码替换（原地修改）：
     * 解密 → 验证可解密 → 写入掩码 "****"，用于响应端脱敏。
     */
    private void decryptAndMask(SyncDataSourceConfig ds) {
        if (ds == null) {
            return;
        }
        if (!isEmpty(ds.getPassword())) {
            if (aesCipher.isEncrypted(ds.getPassword())) {
                aesCipher.decrypt(ds.getPassword()); // 验证可解密
            }
            ds.setPassword(MASK);
        }
        if (ds.getHeaders() != null) {
            Map<String, String> headers = ds.getHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().contains(AUTH_HEADER_PREFIX)
                        && !isEmpty(entry.getValue())) {
                    if (aesCipher.isEncrypted(entry.getValue())) {
                        aesCipher.decrypt(entry.getValue()); // 验证可解密
                    }
                    entry.setValue(MASK);
                }
            }
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 计算 schemaStatus，委托给 SchemaStatusService。
     * 此方法允许测试时通过 Mockito 覆盖。
     */
    SchemaStatus computeSchemaStatus(String subjectCode, List<FieldConfigDTO> activeFields) {
        return schemaStatusService.computeStatus(subjectCode, activeFields);
    }
}
