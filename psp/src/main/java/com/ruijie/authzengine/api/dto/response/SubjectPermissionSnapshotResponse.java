package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 主体权限快照响应 DTO（Q1）。
 */
@Data
@Builder
@Schema(description = "主体权限快照响应")
public class SubjectPermissionSnapshotResponse {

    @Schema(description = "主体拥有的全部权限项编码列表（含角色/岗位/组织间接授权，不含策略过滤）",
        example = "[\"PERM_CONTRACT_READ\", \"PERM_CONTRACT_APPROVE\"]")
    private List<String> permCodes;

    @Schema(description = "引擎评估耗时（毫秒）", example = "8")
    private long evalTimeMs;
}

