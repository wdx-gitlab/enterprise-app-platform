package com.ruijie.authzengine.application.sdk.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限快照结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSnapshotResult {

    private List<String> permCodes;

    private long evalTimeMs;
}