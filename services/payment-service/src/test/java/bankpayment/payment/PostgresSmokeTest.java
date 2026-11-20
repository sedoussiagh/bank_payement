package bankpayment.payment;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresSmokeTest extends PostgresIntegrationTest {
    @Test
    void runsSqlOnPostgres18() throws SQLException {
        try (var connection = connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT 1, current_setting('server_version_num')::integer")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt(1));
            assertEquals(180007, rows.getInt(2));
        }
    }
}
