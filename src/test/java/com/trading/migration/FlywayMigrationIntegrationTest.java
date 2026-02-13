package com.trading.migration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "run.flyway.it", matches = "true")
class FlywayMigrationIntegrationTest {

    @Test
    void migrationsCleanApplyFromScratch() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

            flyway.clean();
            MigrateResult firstRun = flyway.migrate();
            assertTrue(firstRun.migrationsExecuted >= 5, "Expected at least 5 migrations to run");

            flyway.clean();
            MigrateResult secondRun = flyway.migrate();
            assertEquals(firstRun.migrationsExecuted, secondRun.migrationsExecuted);

            try (java.sql.Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true"
             )) {
                assertTrue(resultSet.next());
                assertEquals(secondRun.migrationsExecuted, resultSet.getInt(1));
            }
        }
    }
}
