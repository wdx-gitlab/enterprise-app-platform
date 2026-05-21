package com.ruijie.authzengine.autoconfigure;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.infrastructure.authz.AuthzDecisionHolder;
import com.ruijie.authzengine.infrastructure.authz.BoFieldMappingSupport;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class AuthzRowFilterInterceptorTest {

    @AfterEach
    void tearDown() {
        AuthzDecisionHolder.clear();
    }

    @Test
    void shouldAppendStableOrderByUsingPkColumnsWhenSqlHasNoExplicitOrder() throws Throwable {
        Map<String, Object> rowFilter = new LinkedHashMap<>();
        rowFilter.put("whereClause", "c.owner_id = ?");
        rowFilter.put("tableName", "biz_customer");
        rowFilter.put("params", Collections.singletonList("demo-user"));
        rowFilter.put(BoFieldMappingSupport.ROW_FILTER_PK_COLUMNS_KEY, Collections.singletonList("id"));
        AuthzDecisionHolder.set(AuthzDecision.permit(
            Collections.singletonList("perm"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonMap("rowFilter", rowFilter)));

        Configuration configuration = new Configuration();
        MappedStatement mappedStatement = new MappedStatement.Builder(
            configuration,
            "demoQuery",
            new StaticSqlSource(configuration, "SELECT 1"),
            SqlCommandType.SELECT).build();
        BoundSql boundSql = new BoundSql(
            configuration,
            "SELECT c.id, c.owner_id FROM biz_customer c WHERE c.deleted = 0 LIMIT 10",
            Collections.emptyList(),
            null);

        Executor executor = Mockito.mock(Executor.class);
        ArgumentCaptor<BoundSql> boundSqlCaptor = ArgumentCaptor.forClass(BoundSql.class);
        Mockito.when(executor.query(
            Mockito.eq(mappedStatement),
            Mockito.isNull(),
            Mockito.eq(RowBounds.DEFAULT),
            Mockito.isNull(),
            Mockito.any(CacheKey.class),
            boundSqlCaptor.capture())).thenReturn(Collections.emptyList());

        Method queryMethod = Executor.class.getMethod(
            "query",
            MappedStatement.class,
            Object.class,
            RowBounds.class,
            org.apache.ibatis.session.ResultHandler.class,
            CacheKey.class,
            BoundSql.class);
        Invocation invocation = new Invocation(
            executor,
            queryMethod,
            new Object[] {mappedStatement, null, RowBounds.DEFAULT, null, new CacheKey(), boundSql});

        new AuthzRowFilterInterceptor().intercept(invocation);

        BoundSql rewritten = boundSqlCaptor.getValue();
        Assertions.assertTrue(rewritten.getSql().contains("WHERE (c.owner_id = ?) AND c.deleted = 0"));
        Assertions.assertTrue(rewritten.getSql().contains("ORDER BY c.id LIMIT 10"));
        Assertions.assertEquals("demo-user", rewritten.getAdditionalParameter("__authz_param_0"));
    }
}