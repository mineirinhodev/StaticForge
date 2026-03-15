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
 * Validates that Flyway migration V1 creates all expected tables,
 * columns, constraints, and indexes against a real PostgreSQL instance
 * provisioned automatically via Testcontainers.
 *
 * No external Docker Compose or running database required — the container
 * is started and stopped by the test lifecycle.
 */
@Testcontainers
class V1MigrationTest {

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
    void usersTableExists() throws Exception {
        assertTrue(tableExists("users"), "Table 'users' should exist");
    }

    @Test
    void charactersTableExists() throws Exception {
        assertTrue(tableExists("characters"), "Table 'characters' should exist");
    }

    @Test
    void characterJobsTableExists() throws Exception {
        assertTrue(tableExists("character_jobs"), "Table 'character_jobs' should exist");
    }

    @Test
    void refreshTokensTableExists() throws Exception {
        assertTrue(tableExists("refresh_tokens"), "Table 'refresh_tokens' should exist");
    }

    // ── Users columns ───────────────────────────────────────

    @Test
    void usersTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("users");

        assertAll("users table columns",
                () -> assertTrue(columns.contains("id"), "missing 'id'"),
                () -> assertTrue(columns.contains("email"), "missing 'email'"),
                () -> assertTrue(columns.contains("password_hash"), "missing 'password_hash'"),
                () -> assertTrue(columns.contains("locale"), "missing 'locale'"),
                () -> assertTrue(columns.contains("is_premium"), "missing 'is_premium'"),
                () -> assertTrue(columns.contains("premium_expires_at"), "missing 'premium_expires_at'"),
                () -> assertTrue(columns.contains("is_active"), "missing 'is_active'"),
                () -> assertTrue(columns.contains("oauth_provider"), "missing 'oauth_provider'"),
                () -> assertTrue(columns.contains("oauth_provider_id"), "missing 'oauth_provider_id'"),
                () -> assertTrue(columns.contains("failed_login_attempts"), "missing 'failed_login_attempts'"),
                () -> assertTrue(columns.contains("locked_until"), "missing 'locked_until'"),
                () -> assertTrue(columns.contains("deleted_at"), "missing 'deleted_at'"),
                () -> assertTrue(columns.contains("created_at"), "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"), "missing 'updated_at'")
        );
    }

    // ── Characters columns ──────────────────────────────────

    @Test
    void charactersTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("characters");

        assertAll("characters table columns",
                () -> assertTrue(columns.contains("id"), "missing 'id'"),
                () -> assertTrue(columns.contains("user_id"), "missing 'user_id'"),
                () -> assertTrue(columns.contains("name"), "missing 'name'"),
                () -> assertTrue(columns.contains("server"), "missing 'server'"),
                () -> assertTrue(columns.contains("data_center"), "missing 'data_center'"),
                () -> assertTrue(columns.contains("lodestone_id"), "missing 'lodestone_id'"),
                () -> assertTrue(columns.contains("is_verified"), "missing 'is_verified'"),
                () -> assertTrue(columns.contains("is_main"), "missing 'is_main'"),
                () -> assertTrue(columns.contains("avatar_url"), "missing 'avatar_url'"),
                () -> assertTrue(columns.contains("race"), "missing 'race'"),
                () -> assertTrue(columns.contains("gender"), "missing 'gender'"),
                () -> assertTrue(columns.contains("free_company"), "missing 'free_company'"),
                () -> assertTrue(columns.contains("bio"), "missing 'bio'"),
                () -> assertTrue(columns.contains("looking_for_static"), "missing 'looking_for_static'"),
                () -> assertTrue(columns.contains("availability"), "missing 'availability'"),
                () -> assertTrue(columns.contains("created_at"), "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"), "missing 'updated_at'")
        );
    }

    // ── CharacterJobs columns ───────────────────────────────

    @Test
    void characterJobsTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("character_jobs");

        assertAll("character_jobs table columns",
                () -> assertTrue(columns.contains("id"), "missing 'id'"),
                () -> assertTrue(columns.contains("character_id"), "missing 'character_id'"),
                () -> assertTrue(columns.contains("job_name"), "missing 'job_name'"),
                () -> assertTrue(columns.contains("role"), "missing 'role'"),
                () -> assertTrue(columns.contains("is_main"), "missing 'is_main'"),
                () -> assertTrue(columns.contains("ilvl"), "missing 'ilvl'"),
                () -> assertTrue(columns.contains("parse_tier"), "missing 'parse_tier'"),
                () -> assertTrue(columns.contains("progression"), "missing 'progression'"),
                () -> assertTrue(columns.contains("created_at"), "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"), "missing 'updated_at'")
        );
    }

    // ── RefreshTokens columns ───────────────────────────────

    @Test
    void refreshTokensTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("refresh_tokens");

        assertAll("refresh_tokens table columns",
                () -> assertTrue(columns.contains("id"), "missing 'id'"),
                () -> assertTrue(columns.contains("user_id"), "missing 'user_id'"),
                () -> assertTrue(columns.contains("token_hash"), "missing 'token_hash'"),
                () -> assertTrue(columns.contains("expires_at"), "missing 'expires_at'"),
                () -> assertTrue(columns.contains("created_at"), "missing 'created_at'"),
                () -> assertTrue(columns.contains("revoked_at"), "missing 'revoked_at'"),
                () -> assertTrue(columns.contains("replaced_by"), "missing 'replaced_by'"),
                () -> assertTrue(columns.contains("user_agent"), "missing 'user_agent'"),
                () -> assertTrue(columns.contains("ip_address"), "missing 'ip_address'")
        );
    }

    // ── Constraints ─────────────────────────────────────────

    @Test
    void characterJobsHasUniqueConstraintOnCharacterIdAndJobName() throws Exception {
        assertTrue(constraintExists("character_jobs", "uk_character_job", "UNIQUE"),
                "character_jobs should have UNIQUE constraint 'uk_character_job'");
    }

    @Test
    void charactersHasForeignKeyToUsers() throws Exception {
        assertTrue(foreignKeyExists("characters", "users"),
                "characters should have a FK to users");
    }

    @Test
    void characterJobsHasForeignKeyToCharacters() throws Exception {
        assertTrue(foreignKeyExists("character_jobs", "characters"),
                "character_jobs should have a FK to characters");
    }

    @Test
    void refreshTokensHasForeignKeyToUsers() throws Exception {
        assertTrue(foreignKeyExists("refresh_tokens", "users"),
                "refresh_tokens should have a FK to users");
    }

    // ── Indexes ─────────────────────────────────────────────

    @Test
    void standardIndexesExist() throws Exception {
        assertAll("standard indexes",
                () -> assertTrue(indexExists("idx_users_email"), "missing idx_users_email"),
                () -> assertTrue(indexExists("idx_characters_user_id"), "missing idx_characters_user_id"),
                () -> assertTrue(indexExists("idx_characters_data_center_server"), "missing idx_characters_data_center_server"),
                () -> assertTrue(indexExists("idx_character_jobs_character_id"), "missing idx_character_jobs_character_id"),
                () -> assertTrue(indexExists("idx_refresh_tokens_user_id"), "missing idx_refresh_tokens_user_id"),
                () -> assertTrue(indexExists("idx_refresh_tokens_expires_at"), "missing idx_refresh_tokens_expires_at")
        );
    }

    @Test
    void partialIndexesExist() throws Exception {
        assertAll("partial indexes",
                () -> assertTrue(indexExists("idx_users_oauth"), "missing idx_users_oauth"),
                () -> assertTrue(indexExists("idx_characters_lodestone_id"), "missing idx_characters_lodestone_id"),
                () -> assertTrue(indexExists("idx_characters_lfs"), "missing idx_characters_lfs"),
                () -> assertTrue(indexExists("idx_refresh_tokens_token_hash"), "missing idx_refresh_tokens_token_hash")
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
}
