package org.fluentjdbc.sqlserver;

import org.junit.Assume;
import org.junit.Ignore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SqlServerTests {

    private static final Map<String, String> REPLACEMENTS = new HashMap<>();
    static {
        REPLACEMENTS.put("UUID", "uniqueidentifier");
        REPLACEMENTS.put("INTEGER_PK", "integer identity primary key");
        REPLACEMENTS.put("DATETIME", "datetime");
    }

    public static class DatabaseSaveBuilderTest extends org.fluentjdbc.DatabaseSaveBuilderTest {
        public DatabaseSaveBuilderTest() throws SQLException {
            super(getConnection(), REPLACEMENTS);
        }
    }

    public static class RichDomainModelTest extends org.fluentjdbc.RichDomainModelTest {
        public RichDomainModelTest() throws SQLException {
            super(getConnection(), REPLACEMENTS);
        }

        @Override
        @Ignore
        public void shouldGroupEntriesByTagTypes() {
            // Ignore - relies on ResultTypeMetadata.getTableName, which is not supported
        }
    }

    public static class FluentJdbcDemonstrationTest extends org.fluentjdbc.FluentJdbcDemonstrationTest {
        public FluentJdbcDemonstrationTest() throws SQLException {
            super(getConnection(), REPLACEMENTS);
        }

        @Override
        public void shouldInsertRowWithNonexistantKey() throws SQLException {
            try(Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("set identity_insert demo_table on");
            }

            super.shouldInsertRowWithNonexistantKey();

            try(Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("set identity_insert demo_table off");
            }
        }
    }

    public static class DatabaseTableTest extends org.fluentjdbc.DatabaseTableTest {
        public DatabaseTableTest() throws SQLException {
            super(getConnection(), REPLACEMENTS);
        }

        @Override
        public void shouldInsertWithExplicitKey() throws SQLException {
            try(Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("set identity_insert database_table_test_table on");
            }

            super.shouldInsertWithExplicitKey();

            try(Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("set identity_insert database_table_test_table off");
            }
        }
    }

    public static class BulkInsertTest extends org.fluentjdbc.BulkInsertTest {
        public BulkInsertTest() throws SQLException {
            super(getConnection(), REPLACEMENTS);
        }
    }

    static Connection getConnection() throws SQLException {
        String username = System.getProperty("test.db.sqlserver.username", "fluentjdbc_test");
        String password = System.getProperty("test.db.sqlserver.password", username);
        String url = System.getProperty("test.db.sqlserver.url",
                "jdbc:jtds:sqlserver://localhost:1433;databaseName=" + username);

        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            if (e.getSQLState().equals("08S03")) {
                Assume.assumeNoException(e);
            }
            throw e;
        }
    }
}
