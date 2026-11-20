package bankpayment.payment;

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

class PaymentMigrationIT extends PostgresIntegrationTest {
    @TempDir
    Path migrations;

    @Test
    void installsUpgradesAndRejectsChangedAppliedMigration() throws IOException, SQLException {
        var v1 = copyMigration("/db/migration/V1__payment.sql");

        try (var connection = connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT to_regclass('public.payment_order')")) {
            assertTrue(rows.next());
            assertNull(rows.getString(1));
        }
        assertEquals(1, flyway().migrate().migrationsExecuted);

        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO payment_order (
                        order_id, client_id, idempotency_key, biller_id, invoice_ref, invoice_version,
                        account_id, amount_minor, currency, status, created_at, updated_at
                    ) VALUES (
                        '90000000-0000-4000-8000-000000000001', 'client-demo', 'migration-test',
                        'water-demo', 'demo-001', 1, 'account-demo', 14025, 'MAD', 'RESERVING',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """);
        }

        copyMigration("/db/migration-probe/V2__migration_probe.sql");
        assertEquals(1, flyway().migrate().migrationsExecuted);
        assertTrue(flyway().validateWithResult().validationSuccessful);
        assertEquals(0, flyway().migrate().migrationsExecuted);

        try (var connection = connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery(
                     "SELECT amount_minor, status, migration_probe FROM payment_order WHERE idempotency_key = 'migration-test'")) {
            assertTrue(rows.next());
            assertEquals(14025L, rows.getLong("amount_minor"));
            assertEquals("RESERVING", rows.getString("status"));
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
