package com.ffxiv.planner.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that Flyway migration V2 creates the statics and static_memberships
 * tables with the correct schema, constraints, and indexes against a real
 * PostgreSQL instance provisioned via Testcontainers.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/34
 */
@Testcontainers
class V2MigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static Connection connection;

    @BeforeAll
    static void setUp() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // ── Extension ───────────────────────────────────────────

    @Test
    void pgTrgmExtensionIsEnabled() throws Exception {
        assertTrue(extensionExists("pg_trgm"), "Extension 'pg_trgm' should be enabled");
    }

    // ── Tables exist ────────────────────────────────────────

    @Test
    void staticsTableExists() throws Exception {
        assertTrue(tableExists("statics"), "Table 'statics' should exist");
    }

    @Test
    void staticMembershipsTableExists() throws Exception {
        assertTrue(tableExists("static_memberships"), "Table 'static_memberships' should exist");
    }

    // ── Statics columns ─────────────────────────────────────

    @Test
    void staticsTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("statics");

        assertAll("statics table columns",
                () -> assertTrue(columns.contains("id"),           "missing 'id'"),
                () -> assertTrue(columns.contains("name"),         "missing 'name'"),
                () -> assertTrue(columns.contains("description"),  "missing 'description'"),
                () -> assertTrue(columns.contains("content_scope"), "missing 'content_scope'"),
                () -> assertTrue(columns.contains("loot_rules"),   "missing 'loot_rules'"),
                () -> assertTrue(columns.contains("language"),     "missing 'language'"),
                () -> assertTrue(columns.contains("status"),       "missing 'status'"),
                () -> assertTrue(columns.contains("max_members"),  "missing 'max_members'"),
                () -> assertTrue(columns.contains("data_center"),  "missing 'data_center'"),
                () -> assertTrue(columns.contains("server"),       "missing 'server'"),
                () -> assertTrue(columns.contains("schedule"),     "missing 'schedule'"),
                () -> assertTrue(columns.contains("deleted_at"),   "missing 'deleted_at'"),
                () -> assertTrue(columns.contains("created_at"),   "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"),   "missing 'updated_at'")
        );
    }

    // ── StaticMemberships columns ────────────────────────────

    @Test
    void staticMembershipsTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("static_memberships");

        assertAll("static_memberships table columns",
                () -> assertTrue(columns.contains("id"),           "missing 'id'"),
                () -> assertTrue(columns.contains("static_id"),    "missing 'static_id'"),
                () -> assertTrue(columns.contains("character_id"), "missing 'character_id'"),
                () -> assertTrue(columns.contains("role"),         "missing 'role'"),
                () -> assertTrue(columns.contains("job_assigned"), "missing 'job_assigned'"),
                () -> assertTrue(columns.contains("is_leader"),    "missing 'is_leader'"),
                () -> assertTrue(columns.contains("joined_at"),    "missing 'joined_at'"),
                () -> assertTrue(columns.contains("left_at"),      "missing 'left_at'"),
                () -> assertTrue(columns.contains("created_at"),   "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"),   "missing 'updated_at'")
        );
    }

    // ── Constraints ─────────────────────────────────────────

    @Test
    void staticMembershipsHasUniqueConstraintOnStaticIdAndCharacterId() throws Exception {
        assertTrue(constraintExists("static_memberships", "uk_static_character", "UNIQUE"),
                "static_memberships should have UNIQUE constraint 'uk_static_character'");
    }

    @Test
    void staticMembershipsHasForeignKeyToStatics() throws Exception {
        assertTrue(foreignKeyExists("static_memberships", "statics"),
                "static_memberships should have a FK to statics");
    }

    @Test
    void staticMembershipsHasForeignKeyToCharacters() throws Exception {
        assertTrue(foreignKeyExists("static_memberships", "characters"),
                "static_memberships should have a FK to characters");
    }

    // ── Indexes ─────────────────────────────────────────────

    @Test
    void standardIndexesExist() throws Exception {
        assertAll("standard indexes",
                () -> assertTrue(indexExists("idx_statics_status_dc"),         "missing idx_statics_status_dc"),
                () -> assertTrue(indexExists("idx_memberships_static_id"),      "missing idx_memberships_static_id"),
                () -> assertTrue(indexExists("idx_memberships_character_id"),   "missing idx_memberships_character_id")
        );
    }

    @Test
    void ginTrigramIndexOnStaticsNameExists() throws Exception {
        assertTrue(indexExists("idx_statics_name_trgm"),
                "missing idx_statics_name_trgm (GIN trigram index for name search)");
    }

    // ── Helpers ──────────────────────────────────────────────

    private boolean tableExists(String tableName) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name = ?")) {
            ps.setString(1, tableName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private List<String> getColumnNames(String tableName) throws Exception {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = ?")) {
            ps.setString(1, tableName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                columns.add(rs.getString("column_name"));
            }
        }
        return columns;
    }

    private boolean constraintExists(String tableName, String constraintName, String type) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                        "WHERE table_name = ? AND constraint_name = ? AND constraint_type = ?")) {
            ps.setString(1, tableName);
            ps.setString(2, constraintName);
            ps.setString(3, type);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private boolean foreignKeyExists(String sourceTable, String targetTable) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.referential_constraints rc " +
                        "JOIN information_schema.table_constraints tc " +
                        "ON rc.constraint_name = tc.constraint_name " +
                        "AND rc.constraint_schema = tc.constraint_schema " +
                        "JOIN information_schema.table_constraints tu " +
                        "ON rc.unique_constraint_name = tu.constraint_name " +
                        "AND rc.unique_constraint_schema = tu.constraint_schema " +
                        "WHERE tc.table_name = ? AND tu.table_name = ?")) {
            ps.setString(1, sourceTable);
            ps.setString(2, targetTable);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) >= 1;
        }
    }

    private boolean indexExists(String indexName) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM pg_indexes " +
                        "WHERE schemaname = 'public' AND indexname = ?")) {
            ps.setString(1, indexName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private boolean extensionExists(String extensionName) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = ?")) {
            ps.setString(1, extensionName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) == 1;
        }
    }
}
