package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 权限编码生成与解析服务。
 * <p>统一维护权限项编码四段式规则，避免各链路分散拼接/拆分导致语义漂移。</p>
 * <p>编码格式：</p>
 * <ul>
 *     <li>BO：appCode:bo:boCode:actionCode</li>
 *     <li>API：appCode:api:apiCode:actionCode</li>
 * </ul>
 * <p>为兼容模型级权限，第三段资源编码允许为空字符串，例如 {@code CRM:bo::READ}。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionCodeService {

    private static final String KIND_BO = "bo";

    private static final String KIND_API = "api";

    private final MetaRepository metaRepository;

    private final ResourceRepository resourceRepository;

    /**
     * 根据权限项核心字段生成标准化权限编码。
     *
     * @param tenantId 当前租户
     * @param appCode 当前项目配置的应用编码
     * @param resModelCode 资源模型编码，仅支持 RES_DATA_BO / RES_API
     * @param resId 原始资源标识，允许传资源主键或业务编码；模型级权限可为空
     * @param actionCode 动作编码
     * @return 标准化后的权限编码
     */
    public String generatePermissionCode(String tenantId,
                                         String appCode,
                                         String resModelCode,
                                         String resId,
                                         String actionCode) {
        return resolvePermissionCode(tenantId, appCode, resModelCode, resId, actionCode).getPermCode();
    }

    /**
     * 解析权限项写入请求，返回标准化后的权限编码、资源标识和动作编码。
     */
    public ResolvedPermissionCode resolvePermissionCode(String tenantId,
                                                        String appCode,
                                                        String resModelCode,
                                                        String resId,
                                                        String actionCode) {
        String normalizedAppCode = requireText(appCode, "应用标识不能为空");
        String normalizedActionCode = normalizeActionCode(actionCode);
        if (ResourceModelCode.RES_DATA_BO.name().equals(resModelCode)) {
            ResolvedBoTarget target = resolveBoTarget(tenantId, normalizedAppCode, resId);
            return new ResolvedPermissionCode(
                joinSegments(normalizedAppCode, KIND_BO, target.getBoCode(), normalizedActionCode),
                target.getNormalizedResId(),
                target.getBoCode(),
                normalizedActionCode
            );
        }
        if (ResourceModelCode.RES_API.name().equals(resModelCode)) {
            ResolvedApiTarget target = resolveApiTarget(tenantId, normalizedAppCode, resId);
            return new ResolvedPermissionCode(
                joinSegments(normalizedAppCode, KIND_API, target.getApiCode(), normalizedActionCode),
                target.getNormalizedResId(),
                target.getApiCode(),
                normalizedActionCode
            );
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "资源模型编码不合法，只允许 RES_DATA_BO 或 RES_API，当前值: " + resModelCode);
    }

    /**
     * 解析并校验权限编码四段式结构。
     *
     * @param permCode 权限编码
     * @return 解析结果
     */
    public ParsedPermissionCode parsePermissionCode(String permCode) {
        if (!StringUtils.hasText(permCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限项编码不能为空");
        }
        String[] segments = permCode.split(":", -1);
        if (segments.length != 4) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限项编码格式非法，必须为四段式: " + permCode);
        }
        String appCode = requireText(segments[0], "权限项编码第一段 appCode 不能为空");
        String resourceKind = requireText(segments[1], "权限项编码第二段资源类别不能为空").toLowerCase();
        String actionCode = normalizeActionCode(segments[3]);
        if (!KIND_BO.equals(resourceKind) && !KIND_API.equals(resourceKind)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限项编码第二段只允许 bo 或 api: " + permCode);
        }
        return new ParsedPermissionCode(appCode, resourceKind, segments[2] == null ? "" : segments[2].trim(), actionCode);
    }

    /**
     * 解析 BO 权限编码中的 boCode。
     */
    public String resolveBoCodeFromPermissionCode(String permCode) {
        ParsedPermissionCode parsed = parsePermissionCode(permCode);
        if (!KIND_BO.equals(parsed.getResourceKind())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "RES_DATA_BO 权限项编码第二段必须为 bo: " + permCode);
        }
        return parsed.getResourceCode();
    }

    private ResolvedBoTarget resolveBoTarget(String tenantId, String appCode, String rawResId) {
        if (!StringUtils.hasText(rawResId)) {
            return new ResolvedBoTarget("", "");
        }
        String candidate = rawResId.trim();
        Long numericId = parseLong(candidate);
        if (numericId != null) {
            BoMetaModelDefinition definition = metaRepository.findBoMetaModelById(tenantId, appCode, numericId);
            if (definition != null && StringUtils.hasText(definition.getBoCode())) {
                String normalizedResId = definition.getId() == null ? candidate : String.valueOf(definition.getId());
                return new ResolvedBoTarget(normalizedResId, definition.getBoCode().trim());
            }
        }
        BoMetaModelDefinition definition = metaRepository.findBoMetaModel(tenantId, appCode, candidate);
        if (definition != null && StringUtils.hasText(definition.getBoCode())) {
            String normalizedResId = definition.getId() == null ? candidate : String.valueOf(definition.getId());
            return new ResolvedBoTarget(normalizedResId, definition.getBoCode().trim());
        }
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "业务对象元模型不存在: " + candidate);
    }

    private ResolvedApiTarget resolveApiTarget(String tenantId, String appCode, String rawResId) {
        if (!StringUtils.hasText(rawResId)) {
            return new ResolvedApiTarget("", "");
        }
        String candidate = rawResId.trim();
        Long numericId = parseLong(candidate);
        if (numericId != null) {
            SysResApi api = resourceRepository.findApiById(tenantId, appCode, numericId);
            if (api != null && StringUtils.hasText(api.getApiCode())) {
                return new ResolvedApiTarget(api.getApiCode().trim(), api.getApiCode().trim());
            }
        }
        SysResApi api = resourceRepository.findApi(tenantId, appCode, candidate);
        if (api != null && StringUtils.hasText(api.getApiCode())) {
            return new ResolvedApiTarget(api.getApiCode().trim(), api.getApiCode().trim());
        }
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "API 资源不存在: " + candidate);
    }

    private String normalizeActionCode(String actionCode) {
        return requireText(actionCode, "动作编码不能为空").toUpperCase();
    }

    private String joinSegments(String appCode, String resourceKind, String resourceCode, String actionCode) {
        return appCode + ":" + resourceKind + ":" + (resourceCode == null ? "" : resourceCode) + ":" + actionCode;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 权限编码解析结果。
     */
    @Getter
    public static class ParsedPermissionCode {

        private final String appCode;

        private final String resourceKind;

        private final String resourceCode;

        private final String actionCode;

        public ParsedPermissionCode(String appCode, String resourceKind, String resourceCode, String actionCode) {
            this.appCode = appCode;
            this.resourceKind = resourceKind;
            this.resourceCode = resourceCode;
            this.actionCode = actionCode;
        }
    }

    /**
     * 权限项写入规范化结果。
     */
    @Getter
    public static class ResolvedPermissionCode {

        private final String permCode;

        private final String normalizedResId;

        private final String resourceCode;

        private final String actionCode;

        public ResolvedPermissionCode(String permCode, String normalizedResId, String resourceCode, String actionCode) {
            this.permCode = permCode;
            this.normalizedResId = normalizedResId;
            this.resourceCode = resourceCode;
            this.actionCode = actionCode;
        }
    }

    @Getter
    private static class ResolvedBoTarget {

        private final String normalizedResId;

        private final String boCode;

        private ResolvedBoTarget(String normalizedResId, String boCode) {
            this.normalizedResId = normalizedResId;
            this.boCode = boCode;
        }
    }

    @Getter
    private static class ResolvedApiTarget {

        private final String normalizedResId;

        private final String apiCode;

        private ResolvedApiTarget(String normalizedResId, String apiCode) {
            this.normalizedResId = normalizedResId;
            this.apiCode = apiCode;
        }
    }
}