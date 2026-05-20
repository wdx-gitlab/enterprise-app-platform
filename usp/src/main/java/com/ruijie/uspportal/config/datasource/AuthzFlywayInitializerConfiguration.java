package com.ruijie.uspportal.config.datasource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Configuration
public class AuthzFlywayInitializerConfiguration {

    private static final String AUTHZ_META_MODEL_TABLE = "authz_meta_model";

    @Bean(name = "authzFlywayInitializer")
    @ConditionalOnBean(name = "authzFlyway")
    public FlywayMigrationInitializer authzFlywayInitializer(
        @Qualifier("authzFlyway") Flyway authzFlyway,
        @Value("${authz.engine.flyway.table:authz_flyway_history}") String historyTable,
        @Value("${authz.engine.flyway.baseline-version:2}") String baselineVersion
    ) {
        return new FlywayMigrationInitializer(
            authzFlyway,
            flyway -> migrateAuthzSchema(flyway, historyTable, MigrationVersion.fromVersion(baselineVersion))
        );
    }

    private void migrateAuthzSchema(Flyway flyway, String historyTable, MigrationVersion baselineVersion) {
        DataSource dataSource = flyway.getConfiguration().getDataSource();
        if (dataSource == null) {
            flyway.repair();
            flyway.migrate();
            return;
        }

        flyway.repair();
        if (shouldRebaseline(dataSource, historyTable, baselineVersion)) {
            recreateHistoryTable(dataSource, historyTable);
            flyway.baseline();
        }
        flyway.migrate();
    }

    private boolean shouldRebaseline(DataSource dataSource, String historyTable, MigrationVersion baselineVersion) {
        try (Connection connection = dataSource.getConnection()) {
            boolean authzSchemaExists = tableExists(connection, AUTHZ_META_MODEL_TABLE);
            if (!authzSchemaExists) {
                return false;
            }

            if (!tableExists(connection, historyTable)) {
                return true;
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            Integer successfulSqlMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + historyTable + " WHERE success = 1 AND type = 'SQL'",
                Integer.class
            );
            String currentVersionText = jdbcTemplate.query(
                "SELECT version FROM " + historyTable + " WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank DESC",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null
            );
            MigrationVersion currentVersion = StringUtils.hasText(currentVersionText)
                ? MigrationVersion.fromVersion(currentVersionText)
                : MigrationVersion.EMPTY;
            return successfulSqlMigrationCount != null
                && successfulSqlMigrationCount == 0
                && currentVersion.compareTo(baselineVersion) < 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect authz Flyway state", exception);
        }
    }

    private void recreateHistoryTable(DataSource dataSource, String historyTable) {
        String validatedTableName = validateIdentifier(historyTable);
        new JdbcTemplate(dataSource).execute("DROP TABLE IF EXISTS `" + validatedTableName + "`");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String catalog = connection.getCatalog();
        try (ResultSet resultSet = findTable(connection.getMetaData(), catalog, tableName)) {
            return resultSet.next();
        }
    }

    private ResultSet findTable(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        ResultSet resultSet = metadata.getTables(catalog, null, tableName, new String[] {"TABLE"});
        if (resultSet.next()) {
            return metadata.getTables(catalog, null, tableName, new String[] {"TABLE"});
        }
        resultSet.close();
        return metadata.getTables(catalog, null, tableName.toUpperCase(), new String[] {"TABLE"});
    }

    private String validateIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier) || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unsupported Flyway history table name: " + identifier);
        }
        return identifier;
    }
}