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
 * Validates that Flyway migration V3 creates the static_vacancies, applications,
 * and invitations tables with the correct schema, constraints, and indexes against
 * a real PostgreSQL instance provisioned via Testcontainers.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/34
 */
@Testcontainers
class V3MigrationTest {

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

    // ── Tables exist ────────────────────────────────────────

    @Test
    void staticVacanciesTableExists() throws Exception {
        assertTrue(tableExists("static_vacancies"), "Table 'static_vacancies' should exist");
    }

    @Test
    void applicationsTableExists() throws Exception {
        assertTrue(tableExists("applications"), "Table 'applications' should exist");
    }

    @Test
    void invitationsTableExists() throws Exception {
        assertTrue(tableExists("invitations"), "Table 'invitations' should exist");
    }

    // ── StaticVacancies columns ──────────────────────────────

    @Test
    void staticVacanciesTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("static_vacancies");

        assertAll("static_vacancies table columns",
                () -> assertTrue(columns.contains("id"),              "missing 'id'"),
                () -> assertTrue(columns.contains("static_id"),       "missing 'static_id'"),
                () -> assertTrue(columns.contains("role_needed"),     "missing 'role_needed'"),
                () -> assertTrue(columns.contains("job_preferences"), "missing 'job_preferences'"),
                () -> assertTrue(columns.contains("requirements"),    "missing 'requirements'"),
                () -> assertTrue(columns.contains("description"),     "missing 'description'"),
                () -> assertTrue(columns.contains("is_open"),         "missing 'is_open'"),
                () -> assertTrue(columns.contains("created_at"),      "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"),      "missing 'updated_at'")
        );
    }

    // ── Applications columns ─────────────────────────────────

    @Test
    void applicationsTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("applications");

        assertAll("applications table columns",
                () -> assertTrue(columns.contains("id"),               "missing 'id'"),
                () -> assertTrue(columns.contains("vacancy_id"),       "missing 'vacancy_id'"),
                () -> assertTrue(columns.contains("character_id"),     "missing 'character_id'"),
                () -> assertTrue(columns.contains("message"),          "missing 'message'"),
                () -> assertTrue(columns.contains("status"),           "missing 'status'"),
                () -> assertTrue(columns.contains("applied_at"),       "missing 'applied_at'"),
                () -> assertTrue(columns.contains("responded_at"),     "missing 'responded_at'"),
                () -> assertTrue(columns.contains("response_message"), "missing 'response_message'"),
                () -> assertTrue(columns.contains("created_at"),       "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"),       "missing 'updated_at'")
        );
    }

    // ── Invitations columns ──────────────────────────────────

    @Test
    void invitationsTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("invitations");

        assertAll("invitations table columns",
                () -> assertTrue(columns.contains("id"),           "missing 'id'"),
                () -> assertTrue(columns.contains("static_id"),    "missing 'static_id'"),
                () -> assertTrue(columns.contains("character_id"), "missing 'character_id'"),
                () -> assertTrue(columns.contains("message"),      "missing 'message'"),
                () -> assertTrue(columns.contains("status"),       "missing 'status'"),
                () -> assertTrue(columns.contains("sent_at"),      "missing 'sent_at'"),
                () -> assertTrue(columns.contains("responded_at"), "missing 'responded_at'"),
                () -> assertTrue(columns.contains("vacancy_id"),   "missing 'vacancy_id'"),
                () -> assertTrue(columns.contains("created_at"),   "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"),   "missing 'updated_at'")
        );
    }

    // ── Constraints ─────────────────────────────────────────

    @Test
    void staticVacanciesHasForeignKeyToStatics() throws Exception {
        assertTrue(foreignKeyExists("static_vacancies", "statics"),
                "static_vacancies should have a FK to statics");
    }

    @Test
    void applicationsHasForeignKeyToStaticVacancies() throws Exception {
        assertTrue(foreignKeyExists("applications", "static_vacancies"),
                "applications should have a FK to static_vacancies");
    }

    @Test
    void applicationsHasForeignKeyToCharacters() throws Exception {
        assertTrue(foreignKeyExists("applications", "characters"),
                "applications should have a FK to characters");
    }

    @Test
    void invitationsHasForeignKeyToStatics() throws Exception {
        assertTrue(foreignKeyExists("invitations", "statics"),
                "invitations should have a FK to statics");
    }

    @Test
    void invitationsHasForeignKeyToCharacters() throws Exception {
        assertTrue(foreignKeyExists("invitations", "characters"),
                "invitations should have a FK to characters");
    }

    // ── Indexes ─────────────────────────────────────────────

    @Test
    void vacancyIndexesExist() throws Exception {
        assertTrue(indexExists("idx_vacancies_static_id"),
                "missing idx_vacancies_static_id");
    }

    @Test
    void applicationIndexesExist() throws Exception {
        assertAll("application indexes",
                () -> assertTrue(indexExists("idx_applications_vacancy_status"),   "missing idx_applications_vacancy_status"),
                () -> assertTrue(indexExists("idx_applications_character_status"), "missing idx_applications_character_status"),
                () -> assertTrue(indexExists("idx_applications_applied_at"),       "missing idx_applications_applied_at")
        );
    }

    @Test
    void invitationIndexesExist() throws Exception {
        assertAll("invitation indexes",
                () -> assertTrue(indexExists("idx_invitations_character_status"), "missing idx_invitations_character_status"),
                () -> assertTrue(indexExists("idx_invitations_static_status"),    "missing idx_invitations_static_status"),
                () -> assertTrue(indexExists("idx_invitations_sent_at"),          "missing idx_invitations_sent_at")
        );
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
}
