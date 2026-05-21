package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.BoMetaModelRequest;
import com.ruijie.authzengine.api.dto.request.MetaModelRequest;
import com.ruijie.authzengine.api.dto.request.PolicyTemplateRequest;
import com.ruijie.authzengine.api.dto.request.StandardActionRequest;
import com.ruijie.authzengine.api.dto.response.BoMetaModelResponse;
import com.ruijie.authzengine.api.dto.response.BoSchemaColumnResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.MetaModelResponse;
import com.ruijie.authzengine.api.dto.response.StandardActionResponse;
import com.ruijie.authzengine.api.dto.response.StandardPolicyTemplateResponse;
import com.ruijie.authzengine.application.spi.BoSchemaColumnInfo;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 元模型与标准库装配器。
 */
@Component
public class MetaAssembler {

    public AuthMetaModelDefinition toDefinition(MetaModelRequest request) {
        return AuthMetaModelDefinition.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .modelCode(request.getModelCode())
            .modelName(StringUtils.hasText(request.getModelName()) ? request.getModelName() : request.getModelCode())
            .category(request.getCategory())
            .adapterType(request.getAdapterType())
            .resolver(request.getResolver())
            .schemaView(request.getSchemaView())
            .build();
    }

    public BoMetaModelDefinition toDefinition(BoMetaModelRequest request) {
        return BoMetaModelDefinition.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .boCode(request.getBoCode())
            .boName(request.getBoName())
            .schemaJson(request.getSchemaJson())
            .adapterType(request.getAdapterType())
            .resolver(request.getResolver())
            .build();
    }

    public StandardActionDefinition toDefinition(StandardActionRequest request) {
        return StandardActionDefinition.builder()
            .tenantId(request.getTenantId())
            .actCode(request.getActCode())
            .actName(request.getActName())
            .actType(request.getActType())
            .resCategory(request.getResCategory())
            .riskLevel(request.getRiskLevel())
            .build();
    }

    public StandardPolicyTemplateDefinition toDefinition(PolicyTemplateRequest request) {
        return StandardPolicyTemplateDefinition.builder()
            .tenantId(request.getTenantId())
            .templateCode(request.getTemplateCode())
            .templateName(request.getTemplateName())
            .polType(request.getPolType())
            .expressionScript(request.getExpressionScript())
            .paramSchema(request.getParamSchema())
            .status(request.getStatus())
            .build();
    }

    public MetaModelResponse toResponse(AuthMetaModelDefinition definition) {
        return MetaModelResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .modelCode(definition.getModelCode())
            .modelName(definition.getModelName())
            .category(definition.getCategory())
            .adapterType(definition.getAdapterType())
            .resolver(definition.getResolver())
            .schemaView(definition.getSchemaView())
            .build();
    }

    public BoMetaModelResponse toResponse(BoMetaModelDefinition definition) {
        return BoMetaModelResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .boCode(definition.getBoCode())
            .boName(definition.getBoName())
            .schemaJson(definition.getSchemaJson())
            .adapterType(definition.getAdapterType())
            .resolver(definition.getResolver())
            .build();
    }

    public StandardActionResponse toResponse(StandardActionDefinition definition) {
        return StandardActionResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .actCode(definition.getActCode())
            .actName(definition.getActName())
            .actType(definition.getActType())
            .resCategory(definition.getResCategory())
            .riskLevel(definition.getRiskLevel())
            .build();
    }

    public StandardPolicyTemplateResponse toResponse(StandardPolicyTemplateDefinition definition) {
        return StandardPolicyTemplateResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .templateCode(definition.getTemplateCode())
            .templateName(definition.getTemplateName())
            .polType(definition.getPolType())
            .expressionScript(definition.getExpressionScript())
            .paramSchema(definition.getParamSchema())
            .status(definition.getStatus())
            .build();
    }

    /**
     * 转换分页结果。
     *
     * @param pageResult 领域分页结果
     * @param mapper 记录转换函数
     * @param <T> 领域记录类型
     * @param <R> 响应记录类型
     * @return 接口分页响应
     */
    public <T, R> PageResponse<R> toPageResponse(PageResult<T> pageResult, Function<T, R> mapper) {
        List<R> records = pageResult.getRecords().stream().map(mapper).collect(Collectors.toList());
        return PageResponse.<R>builder()
            .pageNo(pageResult.getPageNo())
            .pageSize(pageResult.getPageSize())
            .total(pageResult.getTotal())
            .records(records)
            .build();
    }

    /**
     * 将 BO 列元数据列表转换为接口响应 DTO 列表。
     *
     * @param columnInfos 采集器返回的列信息列表
     * @return 响应 DTO 列表
     */
    public List<BoSchemaColumnResponse> toSchemaColumnResponses(List<BoSchemaColumnInfo> columnInfos) {
        return columnInfos.stream()
            .map(info -> BoSchemaColumnResponse.builder()
                .tableName(info.getTableName())
                .columnName(info.getColumnName())
                .columnType(info.getColumnType())
                .primaryKey(info.isPrimaryKey())
                .nullable(info.isNullable())
                .comment(info.getComment())
                .build())
            .collect(Collectors.toList());
    }
}