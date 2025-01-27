package org.fluentjdbc;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.fluentjdbc.DatabaseSaveResult.SaveStatus.INSERTED;
import static org.fluentjdbc.DatabaseSaveResult.SaveStatus.UPDATED;

import org.fluentjdbc.DatabaseTable.RowMapper;
import org.fluentjdbc.h2.H2TestDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

public class DatabaseSaveBuilderTest extends AbstractDatabaseTest {

    private DatabaseTable table = new DatabaseTableWithTimestamps("uuid_table");

    private Connection connection;

    public DatabaseSaveBuilderTest() throws SQLException {
        this(createConnection(), H2TestDatabase.REPLACEMENTS);
    }

    protected DatabaseSaveBuilderTest(Connection connection, Map<String, String> replacements) {
        super(replacements);
        this.connection = connection;
    }

    private static Connection createConnection() throws SQLException {
        String jdbcUrl = System.getProperty("test.db.jdbc_url", "jdbc:h2:mem:DatabaseSaveBuilderTest");
        return DriverManager.getConnection(jdbcUrl);
    }

    @Before
    public void openConnection() throws SQLException {
        dropTableIfExists(connection, "uuid_table");
        try(Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    preprocessCreateTable("create table uuid_table (idField ${UUID} primary key, code integer not null, name varchar(50) not null, updated_at ${DATETIME} not null, created_at ${DATETIME} not null)"));
        }
    }

    @After
    public void closeConnection() throws SQLException {
        connection.close();
    }


    @Test
    public void shouldGenerateIdForNewRow() throws Exception {
        String savedName = "demo row";
        UUID id = table
                .newSaveBuilderWithUUID("idField", null)
                .uniqueKey("code", 123)
                .setField("name", savedName)
                .execute(connection)
                .getId();

        String retrievedName = table.where("idField", id).singleString(connection, "name");
        assertThat(retrievedName).isEqualTo(savedName);
    }

    @Test
    public void shouldUpdateRow() throws Exception {
        String savedName = "original row";
        UUID id = table
                .newSaveBuilderWithUUID("idField", null)
                .uniqueKey("code", 123)
                .setField("name", savedName)
                .execute(connection)
                .getId();

        DatabaseSaveResult<UUID> result = table.newSaveBuilderWithUUID("idField", id)
                .uniqueKey("code", 543)
                .setField("name", "updated value")
                .execute(connection);
        assertThat(result.getSaveStatus()).isEqualTo(UPDATED);

        String retrievedName = table.where("idField", id).singleString(connection, "name");
        assertThat(retrievedName).isEqualTo("updated value");

        assertThat(table.where("idField", id).orderBy("name").list(connection, new RowMapper<UUID>() {
            @Override
            public UUID mapRow(@Nonnull DatabaseRow row) throws SQLException {
                return row.getUUID("idField");
            }
        })).containsOnly(id);
    }

    @Test
    public void shouldNotUpdateUnchangedRow() throws SQLException {
        String savedName = "original row";
        UUID firstId = table
                .newSaveBuilderWithUUID("idField", null)
                .uniqueKey("code", 123)
                .setField("name", savedName)
                .execute(connection)
                .getId();

        DatabaseSaveResult<UUID> result = table
                .newSaveBuilderWithUUID("idField", null)
                .uniqueKey("code", 123)
                .setField("name", savedName)
                .execute(connection);
        assertThat(result).isEqualTo(DatabaseSaveResult.unchanged(firstId));
    }

    @Test
    public void shouldUpdateRowOnKey() throws SQLException {
        UUID idOnInsert = table.newSaveBuilderWithUUID("idField", null)
            .uniqueKey("code", 10001)
            .setField("name", "old name")
            .execute(connection)
            .getId();

        DatabaseSaveResult<UUID> result = table.newSaveBuilderWithUUID("idField", null)
                .uniqueKey("code", 10001)
                .setField("name", "new name")
                .execute(connection);
        assertThat(result).isEqualTo(DatabaseSaveResult.updated(idOnInsert));

        assertThat(table.where("idField", idOnInsert).singleString(connection, "name"))
            .isEqualTo("new name");
    }

    @Test
    public void shouldGenerateUsePregeneratedIdForNewRow() throws Exception {
        String savedName = "demo row";
        UUID id = UUID.randomUUID();
        DatabaseSaveResult<UUID> result = table
                .newSaveBuilderWithUUID("idField", id)
                .uniqueKey("code", 123)
                .setField("name", savedName)
                .execute(connection);
        assertThat(result.getSaveStatus()).isEqualTo(INSERTED);
        UUID generatedKey = result.getId();
        assertThat(id).isEqualTo(generatedKey);

        String retrievedName = table.where("idField", id).singleString(connection, "name");
        assertThat(retrievedName).isEqualTo(savedName);
    }

}
