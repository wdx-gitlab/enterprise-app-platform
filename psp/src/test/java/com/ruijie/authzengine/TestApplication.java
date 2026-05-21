package com.ruijie.authzengine;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.ruijie.authzengine.autoconfigure.AuthzEngineAutoConfiguration;
import com.ruijie.authzengine.infrastructure.config.AuditMetaObjectHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 测试引导类，仅用于 authz-engine-core 单模块测试。
 * <p>提供 authzTransactionManager 和 MetaObjectHandler 的后备 bean，
 * 在真实宿主环境中这两个 bean 由 authz-engine-spring-boot-starter 负责注册。
 *
 * <p><b>注意</b>：scanBasePackages 与 {@code AuthzEngineCoreConfiguration} 的 basePackages 保持一致，
 * 刻意<b>不包含</b> {@code com.ruijie.authzengine.shared}，以避免 Spring 处理
 * {@code AuthzEngineCoreConfiguration} 的 {@code @ComponentScan}（其使用了 {@code AuthzBeanNameGenerator}），
 * 从而防止同一批 bean 被重复以 {@code authz.*} 名称注册，导致"required a single bean, but 2 were found"。
 */
@SpringBootApplication(
    // 合并到 starter 模块后，spring.factories 在同一 classpath，必须显式排除 AuthzEngineAutoConfiguration，
    // 否则 @EnableAutoConfiguration 会加载它，AuthzBeanNameGenerator 将以 authz.* 前缀再次注册
    // 相同的 bean，与本类的 @SpringBootApplication 标准扫描产生 NoUniqueBeanDefinitionException。
    exclude = {AuthzEngineAutoConfiguration.class},
    scanBasePackages = {
        "com.ruijie.authzengine.api",
        "com.ruijie.authzengine.application",
        "com.ruijie.authzengine.domain",
        "com.ruijie.authzengine.infrastructure",
        "com.ruijie.authzengine.shared.exception"
    }
)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    /**
     * 测试用专属 ObjectMapper，与 starter 提供的 authzObjectMapper 行为一致。
     */
    @Bean(name = "authzObjectMapper")
    @ConditionalOnMissingBean(name = "authzObjectMapper")
    public ObjectMapper authzObjectMapper() {
        DateTimeFormatter serializer = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter deserializer = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .optionalStart().appendLiteral('T').optionalEnd()
                .optionalStart().appendLiteral(' ').optionalEnd()
                .appendPattern("HH:mm:ss")
                .toFormatter();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(serializer));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(deserializer));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    /**
     * 测试用后备事务管理器，绑定到 H2 默认数据源。
     * 真实部署由 starter 提供同名 bean（绑定专属数据源），此处通过 @ConditionalOnMissingBean 回退。
     */
    @Bean(name = "authzTransactionManager")
    @ConditionalOnMissingBean(name = "authzTransactionManager")
    public PlatformTransactionManager authzTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 测试用后备审计字段填充器。
     * 真实部署由 starter 以 authzMetaObjectHandler 注册，此处提供 MetaObjectHandler 类型 bean。
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler authzMetaObjectHandler() {
        return new AuditMetaObjectHandler();
    }

    /**
     * 测试用后备 NamedParameterJdbcTemplate，绑定到 H2 默认数据源。
     * 真实部署由 starter 提供同名 bean（绑定专属数据源）。
     */
    @Bean(name = "authzNamedParameterJdbcTemplate")
    @ConditionalOnMissingBean(name = "authzNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate authzNamedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
