package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API 路由匹配与重叠检测支持工具。
 *
 * <p>统一封装 HTTP PEP 运行时与启动期校验共用的路由优先级与重叠样本推导逻辑，
 * 避免两处各自维护不同的匹配语义。</p>
 */
final class ApiRouteMatchSupport {

    private static final String METHOD_WILDCARD = "*";

    private static final List<String> GENERIC_SEGMENT_CANDIDATES = Arrays.asList("x", "1", "sample", "item");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    int compareApiMatchPriority(SysResApi left, SysResApi right, String httpMethod, String requestUri) {
        boolean leftExactMethod = isExactMethodMatch(httpMethod, left == null ? null : left.getHttpMethod());
        boolean rightExactMethod = isExactMethodMatch(httpMethod, right == null ? null : right.getHttpMethod());
        if (leftExactMethod != rightExactMethod) {
            return leftExactMethod ? -1 : 1;
        }
        String leftPattern = left == null ? null : left.getUriPattern();
        String rightPattern = right == null ? null : right.getUriPattern();
        return pathMatcher.getPatternComparator(requestUri).compare(leftPattern, rightPattern);
    }

    boolean hasAmbiguousBestMatch(List<SysResApi> matchingApis, String httpMethod, String requestUri) {
        return matchingApis != null
            && matchingApis.size() > 1
            && compareApiMatchPriority(matchingApis.get(0), matchingApis.get(1), httpMethod, requestUri) == 0;
    }

    RouteOverlap findOverlap(SysResApi left, SysResApi right) {
        if (left == null || right == null) {
            return null;
        }
        String requestMethod = resolveOverlapMethod(left.getHttpMethod(), right.getHttpMethod());
        if (!StringUtils.hasText(requestMethod)) {
            return null;
        }
        String requestUri = resolveOverlapUri(left.getUriPattern(), right.getUriPattern());
        if (!StringUtils.hasText(requestUri)) {
            return null;
        }
        if (!pathMatcher.match(left.getUriPattern(), requestUri) || !pathMatcher.match(right.getUriPattern(), requestUri)) {
            return null;
        }
        return new RouteOverlap(requestMethod, requestUri);
    }

    private String resolveOverlapMethod(String leftMethod, String rightMethod) {
        if (!StringUtils.hasText(leftMethod) || !StringUtils.hasText(rightMethod)) {
            return null;
        }
        String normalizedLeft = leftMethod.trim().toUpperCase();
        String normalizedRight = rightMethod.trim().toUpperCase();
        if (METHOD_WILDCARD.equals(normalizedLeft) && METHOD_WILDCARD.equals(normalizedRight)) {
            return "GET";
        }
        if (METHOD_WILDCARD.equals(normalizedLeft)) {
            return normalizedRight;
        }
        if (METHOD_WILDCARD.equals(normalizedRight)) {
            return normalizedLeft;
        }
        return normalizedLeft.equals(normalizedRight) ? normalizedLeft : null;
    }

    private String resolveOverlapUri(String leftPattern, String rightPattern) {
        if (!StringUtils.hasText(leftPattern) || !StringUtils.hasText(rightPattern)) {
            return null;
        }
        String leftSample = instantiatePattern(leftPattern);
        if (StringUtils.hasText(leftSample)
            && pathMatcher.match(leftPattern, leftSample)
            && pathMatcher.match(rightPattern, leftSample)) {
            return leftSample;
        }
        String rightSample = instantiatePattern(rightPattern);
        if (StringUtils.hasText(rightSample)
            && pathMatcher.match(leftPattern, rightSample)
            && pathMatcher.match(rightPattern, rightSample)) {
            return rightSample;
        }
        List<String> leftSegments = tokenize(leftPattern);
        List<String> rightSegments = tokenize(rightPattern);
        List<String> resolved = new ArrayList<>();
        if (!resolveCommonSegments(leftSegments, 0, rightSegments, 0, resolved)) {
            return null;
        }
        return buildPath(resolved);
    }

    private boolean resolveCommonSegments(
        List<String> leftSegments,
        int leftIndex,
        List<String> rightSegments,
        int rightIndex,
        List<String> resolvedSegments
    ) {
        if (leftIndex >= leftSegments.size() && rightIndex >= rightSegments.size()) {
            return true;
        }
        if (leftIndex < leftSegments.size() && isDoubleStar(leftSegments.get(leftIndex))) {
            if (resolveCommonSegments(leftSegments, leftIndex + 1, rightSegments, rightIndex, resolvedSegments)) {
                return true;
            }
            if (rightIndex < rightSegments.size() && !isDoubleStar(rightSegments.get(rightIndex))) {
                String sample = instantiateSegment(rightSegments.get(rightIndex));
                if (StringUtils.hasText(sample)) {
                    resolvedSegments.add(sample);
                    if (resolveCommonSegments(leftSegments, leftIndex, rightSegments, rightIndex + 1, resolvedSegments)) {
                        return true;
                    }
                    resolvedSegments.remove(resolvedSegments.size() - 1);
                }
            }
            return false;
        }
        if (rightIndex < rightSegments.size() && isDoubleStar(rightSegments.get(rightIndex))) {
            if (resolveCommonSegments(leftSegments, leftIndex, rightSegments, rightIndex + 1, resolvedSegments)) {
                return true;
            }
            if (leftIndex < leftSegments.size() && !isDoubleStar(leftSegments.get(leftIndex))) {
                String sample = instantiateSegment(leftSegments.get(leftIndex));
                if (StringUtils.hasText(sample)) {
                    resolvedSegments.add(sample);
                    if (resolveCommonSegments(leftSegments, leftIndex + 1, rightSegments, rightIndex, resolvedSegments)) {
                        return true;
                    }
                    resolvedSegments.remove(resolvedSegments.size() - 1);
                }
            }
            return false;
        }
        if (leftIndex >= leftSegments.size() || rightIndex >= rightSegments.size()) {
            return false;
        }
        String commonSegment = findCommonSegmentSample(leftSegments.get(leftIndex), rightSegments.get(rightIndex));
        if (!StringUtils.hasText(commonSegment)) {
            return false;
        }
        resolvedSegments.add(commonSegment);
        if (resolveCommonSegments(leftSegments, leftIndex + 1, rightSegments, rightIndex + 1, resolvedSegments)) {
            return true;
        }
        resolvedSegments.remove(resolvedSegments.size() - 1);
        return false;
    }

    private String findCommonSegmentSample(String leftSegment, String rightSegment) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, instantiateSegment(leftSegment));
        addCandidate(candidates, instantiateSegment(rightSegment));
        addCandidate(candidates, stripWildcards(leftSegment));
        addCandidate(candidates, stripWildcards(rightSegment));
        candidates.addAll(GENERIC_SEGMENT_CANDIDATES);
        for (String candidate : candidates) {
            if (matchesSegment(leftSegment, candidate) && matchesSegment(rightSegment, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void addCandidate(Set<String> candidates, String candidate) {
        if (StringUtils.hasText(candidate)) {
            candidates.add(candidate);
        }
    }

    private String instantiatePattern(String pattern) {
        List<String> segments = tokenize(pattern);
        if (segments.isEmpty()) {
            return "/";
        }
        List<String> resolved = new ArrayList<>(segments.size());
        for (String segment : segments) {
            resolved.add(instantiateSegment(segment));
        }
        return buildPath(resolved);
    }

    private List<String> tokenize(String pattern) {
        List<String> segments = new ArrayList<>();
        if (!StringUtils.hasText(pattern)) {
            return segments;
        }
        for (String segment : pattern.trim().split("/")) {
            if (StringUtils.hasText(segment)) {
                segments.add(segment.trim());
            }
        }
        return segments;
    }

    private String buildPath(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", segments);
    }

    private String instantiateSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "sample";
        }
        if (isDoubleStar(segment)) {
            return "sample";
        }
        String resolved = segment.replaceAll("\\{[^/]+}", "sample")
            .replace('*', 's')
            .replace('?', 'x');
        if (!StringUtils.hasText(resolved)) {
            return "sample";
        }
        return resolved;
    }

    private String stripWildcards(String segment) {
        if (!StringUtils.hasText(segment) || isDoubleStar(segment)) {
            return null;
        }
        String stripped = segment.replaceAll("\\{[^/]+}", "")
            .replace("*", "")
            .replace("?", "")
            .trim();
        return StringUtils.hasText(stripped) ? stripped : null;
    }

    private boolean matchesSegment(String segmentPattern, String candidate) {
        if (!StringUtils.hasText(segmentPattern) || !StringUtils.hasText(candidate)) {
            return false;
        }
        if (isDoubleStar(segmentPattern)) {
            return true;
        }
        return candidate.matches(toSegmentRegex(segmentPattern));
    }

    private String toSegmentRegex(String segmentPattern) {
        StringBuilder regex = new StringBuilder("^");
        int index = 0;
        while (index < segmentPattern.length()) {
            char current = segmentPattern.charAt(index);
            if (current == '*') {
                regex.append("[^/]*");
                index++;
                continue;
            }
            if (current == '?') {
                regex.append("[^/]");
                index++;
                continue;
            }
            if (current == '{') {
                int endIndex = segmentPattern.indexOf('}', index);
                if (endIndex > index) {
                    regex.append("[^/]+");
                    index = endIndex + 1;
                    continue;
                }
            }
            appendEscaped(regex, current);
            index++;
        }
        regex.append('$');
        return regex.toString();
    }

    private void appendEscaped(StringBuilder regex, char value) {
        if ("\\.^$|()[]{}+".indexOf(value) >= 0) {
            regex.append('\\');
        }
        regex.append(value);
    }

    private boolean isDoubleStar(String segment) {
        return "**".equals(segment);
    }

    private boolean isExactMethodMatch(String requestMethod, String apiMethod) {
        return StringUtils.hasText(apiMethod)
            && !METHOD_WILDCARD.equals(apiMethod.trim())
            && requestMethod.equalsIgnoreCase(apiMethod);
    }

    static final class RouteOverlap {

        private final String httpMethod;

        private final String requestUri;

        RouteOverlap(String httpMethod, String requestUri) {
            this.httpMethod = httpMethod;
            this.requestUri = requestUri;
        }

        String getHttpMethod() {
            return httpMethod;
        }

        String getRequestUri() {
            return requestUri;
        }
    }
}