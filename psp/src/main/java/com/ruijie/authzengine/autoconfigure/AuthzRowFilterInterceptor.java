package com.ruijie.authzengine.autoconfigure;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.infrastructure.authz.AuthzDecisionHolder;
import com.ruijie.authzengine.infrastructure.authz.BoFieldMappingSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.mapping.ParameterMapping;

/**
 * DATA 策略行过滤 MyBatis 拦截器。
 *
 * <p>与 {@link com.ruijie.authzengine.infrastructure.authz.AuthzDecisionHolder} 配合，
 * 在 HTTP 请求处理链内自动读取当前线程绑定的 {@link AuthzDecision#getObligations() obligations}，
 * 将 {@code rowFilter.whereClause} 注入进正在执行的 SELECT SQL，实现行级数据权限的无侵入过滤。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>{@code AuthzHttpPepFilter} 鉴权通过后将决策存入 {@code AuthzDecisionHolder}</li>
 *   <li>业务代码触发 MyBatis 查询时，本拦截器从 ThreadLocal 读取 {@code rowFilter}</li>
 *   <li>检查 SQL 是否涉及 {@code rowFilter.tableName} 对应的表</li>
 *   <li>若匹配，将 {@code whereClause} 注入 SQL WHERE 子句</li>
 *   <li>请求结束后 PEP Filter 清理 ThreadLocal</li>
 * </ol>
 *
 * <p><b>注意</b>：本拦截器仅对宿主应用的 {@code SqlSessionFactory} 生效，
 * authz-engine 专属的 {@code authzSqlSessionFactory} 为手动构建，不受 Spring Boot MyBatis
 * 自动配置管理，因此不会被注入本拦截器。
 */
@Slf4j
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                org.apache.ibatis.cache.CacheKey.class, BoundSql.class})
})
public class AuthzRowFilterInterceptor implements Interceptor {

    private static final String ROW_FILTER_KEY = "rowFilter";

    private static final Pattern TABLE_REFERENCE_PATTERN = Pattern.compile(
        "(?i)\\b(?:FROM|JOIN)\\s+([`\\w.]+)(?:\\s+(?:AS\\s+)?([`\\w]+))?");

    @SuppressWarnings("unchecked")
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 读取当前请求线程绑定的鉴权决策
        AuthzDecision decision = AuthzDecisionHolder.get();
        if (decision == null || decision.getObligations() == null) {
            return invocation.proceed();
        }

        Object rowFilterObj = decision.getObligations().get(ROW_FILTER_KEY);
        if (!(rowFilterObj instanceof Map)) {
            return invocation.proceed();
        }

        Map<String, Object> rowFilter = (Map<String, Object>) rowFilterObj;
        String whereClause = (String) rowFilter.get("whereClause");
        String tableName = (String) rowFilter.get("tableName");
        @SuppressWarnings("unchecked")
        List<Object> filterParams = (List<Object>) rowFilter.get("params");
        List<String> pkColumnNames = toStringList(rowFilter.get(BoFieldMappingSupport.ROW_FILTER_PK_COLUMNS_KEY));

        if (!StringUtils.hasText(whereClause)) {
            return invocation.proceed();
        }

        // 获取当前执行的 BoundSql（6-arg query 的第 6 个参数）
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        BoundSql originalBoundSql = (BoundSql) invocation.getArgs()[5];
        String sql = originalBoundSql.getSql().trim();

        // 仅处理 SELECT 语句
        if (!sql.toUpperCase().startsWith("SELECT")) {
            return invocation.proceed();
        }

        // 若 obligations 指定了表名，则仅对涉及该表的查询注入（防止误伤其他表的查询）
        if (StringUtils.hasText(tableName) && !sqlInvolvesTable(sql, tableName)) {
            return invocation.proceed();
        }

        // 注入 WHERE 条件
        String filteredSql = injectWhereClause(sql, whereClause);
        filteredSql = appendStableOrderBy(filteredSql, tableName, pkColumnNames);
        log.debug("[DATA行过滤] SQL 注入行过滤条件: table={}, whereClause={}, paramsCount={}",
                tableName, whereClause, filterParams == null ? 0 : filterParams.size());

        // 构造新的 ParameterMappings：行过滤参数在前（与注入的 ? 顺序对应），原有参数在后
        List<ParameterMapping> newMappings = new ArrayList<>();
        List<String> authzParamKeys = new ArrayList<>();
        if (filterParams != null) {
            for (int i = 0; i < filterParams.size(); i++) {
                String key = "__authz_param_" + i;
                authzParamKeys.add(key);
                newMappings.add(new ParameterMapping.Builder(
                        ms.getConfiguration(), key, Object.class).build());
            }
        }
        newMappings.addAll(originalBoundSql.getParameterMappings());

        // 构造注入了过滤条件的新 BoundSql
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), filteredSql,
                newMappings, originalBoundSql.getParameterObject());
        copyAdditionalParameters(originalBoundSql, newBoundSql);

        // 将行过滤参数值注入 additionalParameters（MyBatis 优先从 additionalParameters 中按 key 取值）
        for (int i = 0; i < authzParamKeys.size(); i++) {
            newBoundSql.setAdditionalParameter(authzParamKeys.get(i), filterParams.get(i));
        }

        // 用新 BoundSql 替换原参数
        invocation.getArgs()[5] = newBoundSql;
        return invocation.proceed();
    }

    /**
     * 将原 BoundSql 的 additionalParameters 复制到新 BoundSql，
     * 确保动态 SQL 中的 foreach / bind 等参数不丢失。
     */
    @SuppressWarnings("unchecked")
    private void copyAdditionalParameters(BoundSql source, BoundSql target) {
        try {
            Field field = BoundSql.class.getDeclaredField("additionalParameters");
            field.setAccessible(true);
            Map<String, Object> additionalParameters = (Map<String, Object>) field.get(source);
            if (additionalParameters != null) {
                for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
                    target.setAdditionalParameter(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            log.warn("[DATA行过滤] 复制 additionalParameters 失败，不影响主流程: {}", e.getMessage());
        }
    }

    /**
     * 判断 SQL 是否涉及目标表（简单单词边界匹配，避免误匹配子字符串）。
     *
     * @param sql       原始 SQL（大小写均可）
     * @param tableName 目标表名
     * @return true 表示 SQL 涉及该表
     */
    private boolean sqlInvolvesTable(String sql, String tableName) {
        String upper = sql.toUpperCase();
        String target = tableName.toUpperCase();
        int fromPos = upper.indexOf("FROM");
        if (fromPos < 0) {
            return false;
        }
        String afterFrom = upper.substring(fromPos);
        int pos = afterFrom.indexOf(target);
        while (pos >= 0) {
            boolean beforeOk = pos == 0 || !Character.isLetterOrDigit(afterFrom.charAt(pos - 1));
            boolean afterOk = pos + target.length() >= afterFrom.length()
                    || !Character.isLetterOrDigit(afterFrom.charAt(pos + target.length()));
            if (beforeOk && afterOk) {
                return true;
            }
            pos = afterFrom.indexOf(target, pos + 1);
        }
        return false;
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item == null) {
                continue;
            }
            String text = String.valueOf(item).trim();
            if (StringUtils.hasText(text) && !results.contains(text)) {
                results.add(text);
            }
        }
        return results;
    }

    private String appendStableOrderBy(String sql, String tableName, List<String> pkColumnNames) {
        if (!StringUtils.hasText(tableName) || pkColumnNames == null || pkColumnNames.isEmpty()) {
            return sql;
        }
        String upper = sql.toUpperCase();
        if (findKeywordOutsideParens(upper, "ORDER BY") >= 0) {
            return sql;
        }
        String qualifier = resolveTableQualifier(sql, tableName);
        List<String> orderColumns = new ArrayList<>();
        for (String pkColumnName : pkColumnNames) {
            if (!StringUtils.hasText(pkColumnName)) {
                continue;
            }
            orderColumns.add(pkColumnName.contains(".") ? pkColumnName : qualifier + "." + pkColumnName.trim());
        }
        if (orderColumns.isEmpty()) {
            return sql;
        }
        String[] terminators = {"LIMIT", "FOR UPDATE", "FOR SHARE"};
        int insertPos = sql.length();
        for (String keyword : terminators) {
            int pos = findKeywordOutsideParens(upper, keyword);
            if (pos >= 0 && pos < insertPos) {
                insertPos = pos;
            }
        }
        String prefix = sql.substring(0, insertPos).trim();
        String suffix = insertPos >= sql.length() ? "" : " " + sql.substring(insertPos).trim();
        return prefix + " ORDER BY " + String.join(", ", orderColumns) + suffix;
    }

    private String resolveTableQualifier(String sql, String tableName) {
        String normalizedTableName = normalizeIdentifier(tableName);
        Matcher matcher = TABLE_REFERENCE_PATTERN.matcher(sql);
        while (matcher.find()) {
            String tableToken = normalizeIdentifier(matcher.group(1));
            if (!normalizedTableName.equalsIgnoreCase(tableToken)) {
                continue;
            }
            String alias = normalizeIdentifier(matcher.group(2));
            if (StringUtils.hasText(alias) && !isSqlKeyword(alias)) {
                return alias;
            }
            return tableName.trim();
        }
        return tableName.trim();
    }

    private String normalizeIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return "";
        }
        return identifier.trim().replace("`", "");
    }

    private boolean isSqlKeyword(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String upper = token.trim().toUpperCase();
        return "WHERE".equals(upper)
            || "LEFT".equals(upper)
            || "RIGHT".equals(upper)
            || "INNER".equals(upper)
            || "OUTER".equals(upper)
            || "JOIN".equals(upper)
            || "ON".equals(upper)
            || "GROUP".equals(upper)
            || "ORDER".equals(upper)
            || "LIMIT".equals(upper)
            || "HAVING".equals(upper)
            || "FOR".equals(upper);
    }

    /**
     * 将行过滤条件注入 SQL WHERE 子句。
     *
     * <ul>
     *   <li>若已有 WHERE：在 WHERE 关键字后、原有条件前插入 {@code (condition) AND }</li>
     *   <li>若无 WHERE：在 ORDER BY / GROUP BY / LIMIT 等终止关键字之前插入 {@code WHERE (condition)}</li>
     * </ul>
     *
     * @param sql       原始 SQL
     * @param condition WHERE 条件表达式（不含 WHERE 关键字）
     * @return 注入后的 SQL
     */
    private String injectWhereClause(String sql, String condition) {
        String upper = sql.toUpperCase();
        if (!upper.startsWith("SELECT")) {
            return sql;
        }

        // 如果存在外层 WHERE，在其后、原有条件前插入
        int wherePos = findKeywordOutsideParens(upper, "WHERE");
        if (wherePos >= 0) {
            int afterWhere = wherePos + "WHERE".length();
            return sql.substring(0, afterWhere) + " (" + condition + ") AND "
                    + sql.substring(afterWhere).trim();
        }

        // 无 WHERE：在终止关键字之前插入
        String[] terminators = {"ORDER BY", "GROUP BY", "HAVING", "LIMIT", "FOR UPDATE", "FOR SHARE"};
        int insertPos = sql.length();
        for (String kw : terminators) {
            int pos = findKeywordOutsideParens(upper, kw);
            if (pos >= 0 && pos < insertPos) {
                insertPos = pos;
            }
        }
        return sql.substring(0, insertPos).trim() + " WHERE (" + condition + ") "
                + sql.substring(insertPos);
    }

    /**
     * 在 SQL 中查找不在括号内的关键字位置（区分单词边界）。
     *
     * @param upper   已大写的 SQL 字符串
     * @param keyword 要查找的关键字（大写）
     * @return 关键字起始位置，未找到返回 -1
     */
    private int findKeywordOutsideParens(String upper, String keyword) {
        int depth = 0;
        int len = keyword.length();
        for (int i = 0; i <= upper.length() - len; i++) {
            char c = upper.charAt(i);
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                depth--;
                continue;
            }
            if (depth > 0) {
                continue;
            }
            if (upper.startsWith(keyword, i)) {
                boolean beforeOk = i == 0 || !Character.isLetterOrDigit(upper.charAt(i - 1));
                boolean afterOk = i + len >= upper.length()
                        || !Character.isLetterOrDigit(upper.charAt(i + len));
                if (beforeOk && afterOk) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 无需外部参数配置
    }
}
