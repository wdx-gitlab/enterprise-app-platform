package com.ruijie.authzengine.domain.model.decision;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PIP（Policy Information Point）补全后的鉴权上下文。
 * <p>
 * PDP 在做策略匹配前，先由 PIP 根据 AuthzRequest 中的主体和上下文信息
 * 补全完整的身份集合、委托关系和治理属性，形成本对象供后续匹配使用。
 * </p>
 *
 * @see AuthzRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzContext {

    /**
     * 主体身份集合，包含直接身份和间接身份（角色、组织、岗位、用户组等）。
     * <p>PIP 通过 context 传入或从外部身份源查询得到，用于匹配 authz_assignment 中不同 subject_model 的授权记录。</p>
     */
    private Set<SubjectKey> subjectKeys;

    /**
     * 通用属性字典，来源于 AuthzRequest.context 合并 PIP 补全的额外属性。
     * <p>可包含部门层级、IP 白名单、时间窗口等 ABAC 动态属性。</p>
     */
    private Map<String, Object> attributes;

    /**
     * 委托授权标识集合。
     * <p>当主体通过委托（delegation）获得权限时，此处记录委托来源 ID，
     * 匹配结果会体现在 AuthzDecision.matchedDelegateIds 中。</p>
     */
    private Set<String> delegationIds;

    /**
     * 治理属性，由治理层（governance）补充的元数据。
     * <p>例如 BO Hook 的 schemaView、数据范围条件等，在 PDP 匹配后可传递到 obligations。</p>
     */
    private Map<String, Object> governanceAttributes;

    /** 链路追踪标识，与 AuthzRequest.traceId 保持一致。 */
    private String traceId;
}