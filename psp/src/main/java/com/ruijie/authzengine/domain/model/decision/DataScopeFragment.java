package com.ruijie.authzengine.domain.model.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DATA 策略执行后生成的参数化 SQL 片段。
 */
public class DataScopeFragment {

    private final String sql;

    private final List<Object> params;

    /**
     * 构造 DATA 过滤片段。
     *
     * @param sql 参数化 SQL 条件片段，允许为空表示本次不追加过滤
     * @param params 对应 SQL 中问号占位符的参数列表
     */
    public DataScopeFragment(String sql, List<Object> params) {
        this.sql = sql == null ? null : sql.trim();
        this.params = params == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(params));
    }

    /**
     * 返回空过滤片段。
     *
     * @return 不附加任何 SQL 条件的空片段
     */
    public static DataScopeFragment empty() {
        return new DataScopeFragment(null, Collections.emptyList());
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParams() {
        return params;
    }
}