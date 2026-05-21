package com.ruijie.authzengine.domain.service;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 主体展开领域服务。
 *
 * <p>将请求中的原始主体（如用户）与上下文中已合并的关联主体（角色、组织链、岗位、用户组）
 * 归并为一个 SubjectKey 集合，供 PDP 查询授权记录时用作匹配条件。
 *
 * <p>展开规则：
 * <ul>
 *   <li>始终包含原始主体（一般是 SUB_USER）</li>
 *   <li>从 attributes["roles"] 展开 SUB_ROLE 类型主体</li>
 *   <li>从 attributes["orgs"] 展开 SUB_ORG 类型主体（包含向上递归的组织链）</li>
 *   <li>从 attributes["positions"] 展开 SUB_POSITION 类型主体</li>
 *   <li>从 attributes["groups"] 展开 SUB_GROUP 类型主体</li>
 * </ul>
 */
@Service
public class SubjectExpansionService {

    /**
     * 基于请求主体和上下文展开所有可参与授权匹配的主体键。
     *
     * @param request 鉴权请求（提取原始主体）
     * @param attributes 已合并的上下文属性（包含 roles/orgs/positions/groups 等）
     * @return 主体键集合，供 PDP 查询授权记录时使用
     */
    public Set<SubjectKey> expand(AuthzRequest request, Map<String, Object> attributes) {
        Set<SubjectKey> subjectKeys = new LinkedHashSet<>();
        // 始终包含原始主体（如 SUB_USER:demo-user）
        subjectKeys.add(new SubjectKey(request.getSubject().getType(), request.getSubject().getId()));
        // 从上下文属性中展开各类关联主体
        appendSubjectKeys(subjectKeys, attributes, "roles", "SUB_ROLE");
        appendSubjectKeys(subjectKeys, attributes, "orgs", "SUB_ORG");
        appendSubjectKeys(subjectKeys, attributes, "positions", "SUB_POSITION");
        appendSubjectKeys(subjectKeys, attributes, "groups", "SUB_GROUP");
        return subjectKeys;
    }

    private void appendSubjectKeys(Set<SubjectKey> subjectKeys, Map<String, Object> attributes, String key, String subjectType) {
        for (String item : toStringList(attributes.get(key))) {
            subjectKeys.add(new SubjectKey(subjectType, item));
        }
    }

    private Collection<String> toStringList(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof Collection) {
            Collection<?> values = (Collection<?>) rawValue;
            return values.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return Collections.singletonList(String.valueOf(rawValue));
    }
}