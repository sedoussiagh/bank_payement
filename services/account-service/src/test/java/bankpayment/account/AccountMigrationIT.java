package bankpayment.account;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.APPEND;
import static org.flywaydb.core.api.CoreErrorCode.CHECKSUM_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountMigrationIT extends PostgresIntegrationTest {
    @TempDir
    Path migrations;

    @Test
    void installsUpgradesAndRejectsChangedAppliedMigration() throws IOException, SQLException {
        var v1 = copyMigration("/db/migration/V1__account.sql");

        try (var connection = connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT to_regclass('public.account')")) {
            assertTrue(rows.next());
            assertNull(rows.getString(1));
        }
        assertEquals(1, flyway().migrate().migrationsExecuted);

        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO account (account_id, owner_id, kind, status, currency)
                    VALUES ('migration-account', 'client-demo', 'CLIENT', 'ACTIVE', 'MAD')
                    """);
        }

        copyMigration("/db/migration-probe/V2__migration_probe.sql");
        assertEquals(1, flyway().migrate().migrationsExecuted);
        assertTrue(flyway().validateWithResult().validationSuccessful);
        assertEquals(0, flyway().migrate().migrationsExecuted);

        try (var connection = connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery(
                     "SELECT owner_id, booked_minor, migration_probe FROM account WHERE account_id = 'migration-account'")) {
            assertTrue(rows.next());
            assertEquals("client-demo", rows.getString("owner_id"));
            assertEquals(0L, rows.getLong("booked_minor"));
            assertEquals("kept", rows.getString("migration_probe"));
            assertFalse(rows.next());
        }

        Files.writeString(v1, "\nSELECT 42;\n", APPEND);
        var validation = flyway().validateWithResult();
        assertFalse(validation.validationSuccessful);
        assertTrue(validation.invalidMigrations.stream().anyMatch(migration ->
                "1".equals(migration.version) && migration.errorDetails.errorCode == CHECKSUM_MISMATCH));
        assertThrows(FlywayException.class, () -> flyway().migrate());
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + migrations.toAbsolutePath())
                .load();
    }

    private Path copyMigration(String resource) throws IOException {
        var target = migrations.resolve(resource.substring(resource.lastIndexOf('/') + 1));
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream(resource), resource)) {
            Files.copy(input, target);
        }
        return target;
    }
}
