package com.ruijie.dapengine.admin.service;

import com.ruijie.dapengine.common.enums.FieldType;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.FieldConfigRequest;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SubjectRequest;
import com.ruijie.dapengine.common.util.FieldSchemaHelper;
import com.ruijie.dapengine.repository.MetadataRepository;
import com.ruijie.dapengine.repository.SubjectRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 主数据类型与字段元数据管理核心业务服务。
 * saveSubjectConfig() 实现 10 步流程，全程 @Transactional。
 */
public class MetadataConfigService {

    /**
     * Subject Code 校验正则：大写字母开头，仅允许大写字母、数字、下划线，长度 2–30。
     * 示例：{@code USER_PROFILE}、{@code DEVICE_ASSET}
     */
    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    /**
     * 字段名校验正则：小写字母开头，仅允许小写字母、数字、下划线，长度 1–128。
     * 示例：{@code employee_id}、{@code dept_code}
     */
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,127}$");

    /**
     * 系统保留字段名集合：这些字段名由 DAP 平台框架内置实现，
     * 不允许业务层加为自定义字段，防止列名冲突。
     */
    private static final Set<String> RESERVED_FIELD_NAMES = new HashSet<>(Arrays.asList(
        "id", "tenant_id", "app_code", "code", "name", "parent_code",
        "dap_version", "dap_sync_time", "is_delete",
        "created_at", "updated_at", "created_by", "updated_by"
    ));

    /**
     * 系统内置字段名集合——对应每个子题 Subject 动态表中固定存在的列。
     * 这些字段在 {@link com.ruijie.dapengine.common.model.FieldConfigDTO#isSystem()} 返回为 {@code true}，
     * 不允许被删除或类型修改。
     */
    private static final Set<String> SYSTEM_FIELD_NAMES = new HashSet<>(Arrays.asList(
        "code", "name", "parent_code"
    ));

    private static final int MAX_ALLOWED_VARCHAR_LENGTH = FieldSchemaHelper.MAX_VARCHAR_LENGTH;

    /**
     * 字段类型展宽规则映射表。
     * Key 为当前类型，Value 为允许变更的目标类型集合。
     * <p>展宽规则：{@code STRING} → {@code STRING_LONG/TEXT}，
     * {@code STRING_LONG} → {@code TEXT}，{@code INT} → {@code DECIMAL}，
     * {@code DATE} → {@code DATETIME}。未列出的类型组合均为禁止变更。</p>
     */
    private static final Map<FieldType, Set<FieldType>> WIDENING_RULES;
    static {
        WIDENING_RULES = new HashMap<>();
        WIDENING_RULES.put(FieldType.STRING, new HashSet<>(Arrays.asList(FieldType.STRING_LONG, FieldType.TEXT)));
        WIDENING_RULES.put(FieldType.STRING_LONG, new HashSet<>(Collections.singletonList(FieldType.TEXT)));
        WIDENING_RULES.put(FieldType.INT, new HashSet<>(Collections.singletonList(FieldType.DECIMAL)));
        WIDENING_RULES.put(FieldType.DATE, new HashSet<>(Collections.singletonList(FieldType.DATETIME)));
    }

    private final SubjectRepository subjectRepository;
    private final MetadataRepository metadataRepository;
    private final SchemaStatusService schemaStatusService;

    public MetadataConfigService(SubjectRepository subjectRepository,
                                 MetadataRepository metadataRepository,
                                 SchemaStatusService schemaStatusService) {
        this.subjectRepository = subjectRepository;
        this.metadataRepository = metadataRepository;
        this.schemaStatusService = schemaStatusService;
    }

    // -------------------------------------------------------------------------
    // US1: Subject 生命周期管理
    // -------------------------------------------------------------------------

    /**
     * 查询所有未逻辑删除的 Subject，并附带动态计算的 schemaStatus。
     */
    public List<SubjectDTO> listSubjects() {
        List<SubjectDTO> subjects = subjectRepository.listActive();
        for (SubjectDTO subject : subjects) {
            List<FieldConfigDTO> activeFields = metadataRepository.findActiveBySubjectId(subject.getId());
            subject.setSchemaStatus(schemaStatusService.computeStatus(subject.getCode(), activeFields));
        }
        return subjects;
    }

    /**
     * 逻辑删除 Subject（is_delete=1），同时逻辑删除对应的 sync_config（后续阶段实现）。
     *
     * @throws DapValidationException 如果 subject 不存在或已删除
     */
    public void deleteSubject(String subjectCode, String operatorId) {
        SubjectDTO existing = subjectRepository.findByCode(subjectCode);
        if (existing == null || (existing.getIsDelete() != null && existing.getIsDelete() == 1)) {
            throw new DapValidationException("[DAP Engine] subject '" + subjectCode + "' 不存在或已删除");
        }
        if (existing.isBuiltIn()) {
            throw new DapValidationException("[DAP Engine] 内置主题 '" + subjectCode + "' 不允许删除");
        }
        int rows = subjectRepository.logicDelete(subjectCode);
        if (rows == 0) {
            throw new DapValidationException("[DAP Engine] subject '" + subjectCode + "' 删除失败");
        }
        // 同步逻辑删除 sync_config（Phase 4 实现，此处预留扩展点）
    }

    // -------------------------------------------------------------------------
    // US2: saveSubjectConfig — 10 步流程
    // -------------------------------------------------------------------------

    /**
     * 创建或更新 Subject 及其字段配置（saveSubjectConfig 10 步流程）。
     *
     * @param subjectCode PUT 时由路径参数提供；POST 时从 request.subject.code 取
     * @param request     请求体
     * @param operatorId  操作人（来自 X-User-Id 请求头）
     * @return 保存后的完整 SubjectDTO
     */
    @Transactional("dapTransactionManager")
    public SubjectDTO saveSubjectConfig(String subjectCode, SubjectRequest request, String operatorId) {
        SubjectRequest.SubjectInfo info = request.getSubject();
        List<FieldConfigRequest> requestedFields = request.getFields() != null ? request.getFields() : Collections.<FieldConfigRequest>emptyList();

        // ① 校验 subject 基础信息
        validateSubjectInfo(subjectCode, info);
        validateFields(requestedFields);

        // ② 查询是否为新建 subject
        SubjectDTO existing = subjectRepository.findByCode(subjectCode);
        boolean isNew = (existing == null);

        // 更新时 subjectCode 不可修改（path param 与请求体 code 必须一致）
        if (!isNew && info.getCode() != null && !info.getCode().equals(subjectCode)) {
            throw new DapValidationException(
                "[DAP Engine] subject.code 不可修改：当前值=" + subjectCode + "，请求体 code=" + info.getCode());
        }

        long subjectId;
        if (isNew) {
            // ③ 新建：INSERT dap_sys_subject
            subjectId = subjectRepository.insert(
                subjectCode, info.getName(), info.getDescription(),
                info.isTree(), info.getStatus(), operatorId
            );
        } else {
            // ③ 更新：UPDATE dap_sys_subject
            subjectId = existing.getId();
            subjectRepository.update(
                subjectCode, info.getName(), info.getDescription(),
                info.isTree(), info.getStatus(), operatorId
            );
        }

        // ④ 自动补齐系统字段元数据
        upsertSystemFields(subjectId, subjectCode, info.getName(), info.isTree(), operatorId);

        // ⑤ 比较旧字段集与新字段集，执行 ⑥⑦⑧⑨
        processCustomFields(subjectId, subjectCode, info.getName(), requestedFields, operatorId);

        // ⑩ 更新 updated_at（字段变更时）
        subjectRepository.touchUpdatedAt(subjectId);

        // 构建并返回完整 SubjectDTO
        return buildSubjectDTO(subjectCode, subjectId, info);
    }

    // -------------------------------------------------------------------------
    // Internal: 校验逻辑
    // -------------------------------------------------------------------------

    void validateSubjectCode(String code) {
        if (code == null || !SUBJECT_CODE_PATTERN.matcher(code).matches()) {
            throw new DapValidationException(
                "[DAP Engine] subject.code 格式不合法，必须匹配 ^[A-Z][A-Z0-9_]{1,29}$，实际值：" + code);
        }
        // 拒绝以 DAP_ 开头的 code：动态表名规则为 dap_{code.toLowerCase()}，
        // 若 code = DAP_SYS_ORG 则表名变为 dap_dap_sys_org（双前缀），产生歧义。
        if (code.startsWith("DAP_")) {
            throw new DapValidationException(
                "[DAP Engine] subject.code 不允许以 'DAP_' 开头，该前缀为系统动态表保留前缀，"
                + "会导致动态表名产生双前缀（dap_dap_...）。实际值：" + code);
        }
    }

    private void validateSubjectInfo(String code, SubjectRequest.SubjectInfo info) {
        validateSubjectCode(code);
        if (info == null || info.getName() == null || info.getName().trim().isEmpty()) {
            throw new DapValidationException("[DAP Engine] subject.name 不能为空");
        }
        if (info.getName().length() > 128) {
            throw new DapValidationException("[DAP Engine] subject.name 长度不能超过 128 字符");
        }
    }

    private void validateFields(List<FieldConfigRequest> fields) {
        for (FieldConfigRequest field : fields) {
            String fn = field.getFieldName();
            if (fn == null || !FIELD_NAME_PATTERN.matcher(fn).matches()) {
                throw new DapValidationException(
                    "[DAP Engine] fieldName 格式不合法，必须匹配 ^[a-z][a-z0-9_]{0,127}$，实际值：" + fn);
            }
            if (RESERVED_FIELD_NAMES.contains(fn)) {
                throw new DapValidationException(
                    "[DAP Engine] fieldName '" + fn + "' 是系统保留字，不允许作为自定义字段名");
            }
            if ("ENUM".equals(field.getFieldType())) {
                if (field.getDictCode() == null || field.getDictCode().trim().isEmpty()) {
                    throw new DapValidationException(
                        "[DAP Engine] 字段 " + fn + " 类型为 ENUM，dictCode 不能为空");
                }
            }
            // 校验 fieldType 是否为合法枚举值
            try {
                FieldType.valueOf(field.getFieldType());
            } catch (IllegalArgumentException e) {
                throw new DapValidationException(
                    "[DAP Engine] 字段 " + fn + " 的 fieldType '" + field.getFieldType() + "' 不合法");
            }
            Integer normalizedMaxLength = FieldSchemaHelper.normalizeMaxLength(field.getFieldType(), field.getMaxLength());
            if (FieldSchemaHelper.isVarcharFamily(field.getFieldType())) {
                if (normalizedMaxLength == null || normalizedMaxLength <= 0) {
                    throw new DapValidationException("[DAP Engine] 字段 " + fn + " 的最大长度必须大于 0");
                }
                if (normalizedMaxLength > MAX_ALLOWED_VARCHAR_LENGTH) {
                    throw new DapValidationException("[DAP Engine] 字段 " + fn + " 的最大长度不能超过 " + MAX_ALLOWED_VARCHAR_LENGTH);
                }
                field.setMaxLength(normalizedMaxLength);
            } else if ("TEXT".equals(field.getFieldType())) {
                field.setMaxLength(FieldSchemaHelper.DEFAULT_TEXT_LENGTH);
            } else {
                field.setMaxLength(0);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal: 系统字段补齐
    // -------------------------------------------------------------------------

    private void upsertSystemFields(long subjectId, String subjectCode, String subjectName,
                                    boolean isTree, String operatorId) {
        upsertSystemField(subjectId, subjectCode, subjectName, "code", "编码", -100, operatorId);
        upsertSystemField(subjectId, subjectCode, subjectName, "name", "名称", -99, operatorId);
        if (isTree) {
            upsertSystemField(subjectId, subjectCode, subjectName, "parent_code", "上级编码", -98, operatorId);
        }
    }

    private void upsertSystemField(long subjectId, String subjectCode, String subjectName,
                                   String fieldName, String fieldLabel, int sortOrder, String operatorId) {
        FieldConfigDTO existing = metadataRepository.findBySubjectIdAndFieldName(subjectId, fieldName);
        if (existing == null) {
            metadataRepository.insertField(subjectId, subjectCode, subjectName,
                fieldName, "STRING", FieldSchemaHelper.DEFAULT_STRING_LENGTH,
                fieldLabel, false, null, sortOrder, true, operatorId);
        } else if (existing.getIsDelete() == 1) {
            // 如果系统字段曾被意外废弃，恢复它
            metadataRepository.setIsDelete(existing.getId(), 0, operatorId);
        }
    }

    // -------------------------------------------------------------------------
    // Internal: 自定义字段处理（⑤-⑨）
    // -------------------------------------------------------------------------

    private void processCustomFields(long subjectId, String subjectCode, String subjectName,
                                     List<FieldConfigRequest> requestedFields, String operatorId) {
        // 获取当前数据库中全部字段（含废弃）
        List<FieldConfigDTO> dbFields = metadataRepository.findBySubjectId(subjectId);

        // 构建 map：fieldName -> 数据库记录（仅自定义字段）
        Map<String, FieldConfigDTO> dbCustomMap = new LinkedHashMap<>();
        for (FieldConfigDTO f : dbFields) {
            if (!SYSTEM_FIELD_NAMES.contains(f.getFieldName())) {
                dbCustomMap.put(f.getFieldName(), f);
            }
        }

        // 构建请求字段 map（保持顺序，用于 sort_order 自动分配）
        Map<String, FieldConfigRequest> reqMap = new LinkedHashMap<>();
        int autoSortOrder = 1;
        for (FieldConfigRequest req : requestedFields) {
            if (req.getSortOrder() == 0) {
                req.setSortOrder(autoSortOrder * 10);
            }
            autoSortOrder++;
            reqMap.put(req.getFieldName(), req);
        }

        // ⑥ 新增字段：请求中有，数据库中没有
        // ⑨ 重激活废弃字段：请求中有，数据库中已废弃
        for (Map.Entry<String, FieldConfigRequest> entry : reqMap.entrySet()) {
            String fieldName = entry.getKey();
            FieldConfigRequest req = entry.getValue();
            FieldConfigDTO dbField = dbCustomMap.get(fieldName);

            if (dbField == null) {
                // ⑥ 新增
                metadataRepository.insertField(subjectId, subjectCode, subjectName,
                    fieldName, req.getFieldType(), req.getMaxLength(), req.getFieldLabel(),
                    req.isRequired(), req.getDictCode(), req.getSortOrder(), false, operatorId);
            } else if (dbField.getIsDelete() == 1) {
                // ⑨ 重激活废弃字段
                metadataRepository.setIsDelete(dbField.getId(), 0, operatorId);
                validateTypeWidening(fieldName, dbField, req);
                metadataRepository.updateField(dbField.getId(),
                    req.getFieldType(), req.getFieldLabel(), req.getMaxLength(),
                    req.isRequired(), req.getDictCode(), req.getSortOrder(), operatorId);
            } else {
                // ⑦ 更新已存在字段（仅允许修改 label/required/dictCode/sortOrder/兼容扩容 type）
                validateTypeWidening(fieldName, dbField, req);
                metadataRepository.updateField(dbField.getId(),
                    req.getFieldType(), req.getFieldLabel(), req.getMaxLength(),
                    req.isRequired(), req.getDictCode(), req.getSortOrder(), operatorId);
            }
        }

        // ⑧ 废弃字段：数据库中有（且 is_delete=0），请求中没有
        for (Map.Entry<String, FieldConfigDTO> entry : dbCustomMap.entrySet()) {
            String fieldName = entry.getKey();
            FieldConfigDTO dbField = entry.getValue();
            if (dbField.getIsDelete() == 0 && !reqMap.containsKey(fieldName)) {
                metadataRepository.setIsDelete(dbField.getId(), 1, operatorId);
            }
        }
    }

    private void validateTypeWidening(String fieldName, FieldConfigDTO dbField, FieldConfigRequest requestField) {
        String oldType = dbField.getFieldType();
        String newType = requestField.getFieldType();
        Integer oldMaxLength = FieldSchemaHelper.normalizeMaxLength(oldType, dbField.getMaxLength());
        Integer newMaxLength = FieldSchemaHelper.normalizeMaxLength(newType, requestField.getMaxLength());
        if (oldType.equals(newType)) {
            validateMaxLengthWidening(fieldName, oldType, oldMaxLength, newMaxLength);
            return; // 相同类型，允许
        }
        if (SYSTEM_FIELD_NAMES.contains(fieldName)) {
            throw new DapValidationException(
                "[DAP Engine] 系统字段 " + fieldName + " 为只读字段，不允许修改 fieldType");
        }
        FieldType oldFt;
        FieldType newFt;
        try {
            oldFt = FieldType.valueOf(oldType);
            newFt = FieldType.valueOf(newType);
        } catch (IllegalArgumentException e) {
            throw new DapValidationException("[DAP Engine] 字段 " + fieldName + " 类型值不合法");
        }
        Set<FieldType> allowed = WIDENING_RULES.get(oldFt);
        if (allowed == null || !allowed.contains(newFt)) {
            throw new DapValidationException(
                "[DAP Engine] 字段 " + fieldName + " 类型变更不安全（" + oldType + " → " + newType + "），仅允许兼容性扩容");
        }
        validateMaxLengthWidening(fieldName, oldType, oldMaxLength, newMaxLength);
    }

    private void validateMaxLengthWidening(String fieldName, String oldType, Integer oldMaxLength, Integer newMaxLength) {
        if (!FieldSchemaHelper.isVarcharFamily(oldType)) {
            return;
        }
        if (oldMaxLength == null || newMaxLength == null) {
            return;
        }
        if (newMaxLength < oldMaxLength) {
            throw new DapValidationException(
                "[DAP Engine] 字段 " + fieldName + " 最大长度不允许缩小（" + oldMaxLength + " → " + newMaxLength + "）");
        }
    }

    // -------------------------------------------------------------------------
    // Internal: 构建响应 DTO
    // -------------------------------------------------------------------------

    private SubjectDTO buildSubjectDTO(String subjectCode, long subjectId, SubjectRequest.SubjectInfo info) {
        List<FieldConfigDTO> allFields = metadataRepository.findBySubjectId(subjectId);
        List<FieldConfigDTO> activeFields = new ArrayList<>();
        for (FieldConfigDTO f : allFields) {
            if (f.getIsDelete() == 0) {
                activeFields.add(f);
            }
            // 标记系统字段
            f.setSystem(SYSTEM_FIELD_NAMES.contains(f.getFieldName()));
        }

        SchemaStatus status = schemaStatusService.computeStatus(subjectCode, activeFields);

        SubjectDTO dto = new SubjectDTO();
        dto.setId(subjectId);
        dto.setCode(subjectCode);
        dto.setName(info.getName());
        dto.setDescription(info.getDescription());
        dto.setTree(info.isTree());
        dto.setStatus(info.getStatus());
        dto.setSchemaStatus(status);
        dto.setFields(allFields);
        return dto;
    }

    /**
     * 获取 subject 字段列表（含废弃）。
     */
    public SubjectDTO getFieldsBySubject(String subjectCode) {
        SubjectDTO subject = subjectRepository.findByCode(subjectCode);
        if (subject == null) {
            throw new DapValidationException("[DAP Engine] subject '" + subjectCode + "' 不存在或已删除");
        }
        List<FieldConfigDTO> allFields = metadataRepository.findBySubjectId(subject.getId());
        for (FieldConfigDTO f : allFields) {
            f.setSystem(SYSTEM_FIELD_NAMES.contains(f.getFieldName()));
        }

        List<FieldConfigDTO> activeFields = new ArrayList<>();
        for (FieldConfigDTO f : allFields) {
            if (f.getIsDelete() == 0) {
                activeFields.add(f);
            }
        }
        subject.setSchemaStatus(schemaStatusService.computeStatus(subjectCode, activeFields));
        subject.setFields(allFields);
        return subject;
    }

    // -------------------------------------------------------------------------
    // Phase 3: DapEngineSchemaInitializer 所需方法
    // -------------------------------------------------------------------------

    /**
     * 校验 subject 存在且有效（is_delete=0），不存在或已删除则抛出 DapValidationException。
     * 供 {@link com.ruijie.dapengine.migration.DapEngineSchemaInitializer} 调用。
     *
     * @param subjectCode Subject code
     * @throws DapValidationException subject 不存在或已删除时
     */
    public void validateSubject(String subjectCode) {
        SubjectDTO subject = subjectRepository.findByCode(subjectCode);
        if (subject == null || (subject.getIsDelete() != null && subject.getIsDelete() == 1)) {
            throw new DapValidationException("[DAP Engine] subject '" + subjectCode + "' 不存在或已删除");
        }
    }

    /**
     * 返回所有有效（is_delete=0）Subject 的 code 列表，供启动兜底遍历使用。
     *
     * @return Subject code 列表
     */
    public List<String> listSubjectCodes() {
        List<SubjectDTO> subjects = subjectRepository.listActive();
        List<String> codes = new ArrayList<>();
        for (SubjectDTO s : subjects) {
            codes.add(s.getCode());
        }
        return codes;
    }

    /**
     * 返回指定 Subject 的有效（is_delete=0）字段列表（含系统字段），用于 applySchema。
     *
     * @param subjectCode Subject code
     * @return 有效字段列表
     * @throws DapValidationException subject 不存在时
     */
    public List<FieldConfigDTO> getActiveFieldDTOs(String subjectCode) {
        SubjectDTO subject = subjectRepository.findByCode(subjectCode);
        if (subject == null) {
            throw new DapValidationException("[DAP Engine] subject '" + subjectCode + "' 不存在");
        }
        return metadataRepository.findActiveBySubjectId(subject.getId());
    }
}

