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
 * Validates that Flyway migration V8 creates subscriptions and
 * payment_history tables with the correct schema.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/24
 */
@Testcontainers
class V8MigrationTest {

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
    void subscriptionsTableExists() throws Exception {
        assertTrue(tableExists("subscriptions"), "Table 'subscriptions' should exist");
    }

    @Test
    void paymentHistoryTableExists() throws Exception {
        assertTrue(tableExists("payment_history"), "Table 'payment_history' should exist");
    }

    // ── Subscriptions columns ───────────────────────────────

    @Test
    void subscriptionsTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("subscriptions");

        assertAll("subscriptions table columns",
                () -> assertTrue(columns.contains("id"),                    "missing 'id'"),
                () -> assertTrue(columns.contains("user_id"),               "missing 'user_id'"),
                () -> assertTrue(columns.contains("plan"),                  "missing 'plan'"),
                () -> assertTrue(columns.contains("status"),                "missing 'status'"),
                () -> assertTrue(columns.contains("start_date"),            "missing 'start_date'"),
                () -> assertTrue(columns.contains("end_date"),              "missing 'end_date'"),
                () -> assertTrue(columns.contains("cancel_at_period_end"),  "missing 'cancel_at_period_end'"),
                () -> assertTrue(columns.contains("trial_used"),            "missing 'trial_used'"),
                () -> assertTrue(columns.contains("stripe_customer_id"),    "missing 'stripe_customer_id'"),
                () -> assertTrue(columns.contains("stripe_subscription_id"), "missing 'stripe_subscription_id'"),
                () -> assertTrue(columns.contains("auto_renew"),            "missing 'auto_renew'"),
                () -> assertTrue(columns.contains("created_at"),            "missing 'created_at'"),
                () -> assertTrue(columns.contains("updated_at"),            "missing 'updated_at'")
        );
    }

    // ── PaymentHistory columns ──────────────────────────────

    @Test
    void paymentHistoryTableHasExpectedColumns() throws Exception {
        List<String> columns = getColumnNames("payment_history");

        assertAll("payment_history table columns",
                () -> assertTrue(columns.contains("id"),                "missing 'id'"),
                () -> assertTrue(columns.contains("subscription_id"),   "missing 'subscription_id'"),
                () -> assertTrue(columns.contains("amount"),            "missing 'amount'"),
                () -> assertTrue(columns.contains("currency"),          "missing 'currency'"),
                () -> assertTrue(columns.contains("stripe_payment_id"), "missing 'stripe_payment_id'"),
                () -> assertTrue(columns.contains("stripe_invoice_id"), "missing 'stripe_invoice_id'"),
                () -> assertTrue(columns.contains("status"),            "missing 'status'"),
                () -> assertTrue(columns.contains("paid_at"),           "missing 'paid_at'"),
                () -> assertTrue(columns.contains("failure_reason"),    "missing 'failure_reason'"),
                () -> assertTrue(columns.contains("created_at"),        "missing 'created_at'")
        );
    }

    // ── Constraints ─────────────────────────────────────────

    @Test
    void subscriptionsHasUniqueConstraintOnUserId() throws Exception {
        assertTrue(uniqueConstraintExists("subscriptions", "user_id"),
                "subscriptions should have UNIQUE constraint on 'user_id'");
    }

    @Test
    void subscriptionsHasUniqueConstraintOnStripeSubscriptionId() throws Exception {
        assertTrue(uniqueConstraintExists("subscriptions", "stripe_subscription_id"),
                "subscriptions should have UNIQUE constraint on 'stripe_subscription_id'");
    }

    @Test
    void subscriptionsHasForeignKeyToUsers() throws Exception {
        assertTrue(foreignKeyExists("subscriptions", "users"),
                "subscriptions should have a FK to users");
    }

    @Test
    void paymentHistoryHasForeignKeyToSubscriptions() throws Exception {
        assertTrue(foreignKeyExists("payment_history", "subscriptions"),
                "payment_history should have a FK to subscriptions");
    }

    // ── Indexes ─────────────────────────────────────────────

    @Test
    void standardIndexesExist() throws Exception {
        assertAll("standard indexes",
                () -> assertTrue(indexExists("idx_subscriptions_user_id"),              "missing idx_subscriptions_user_id"),
                () -> assertTrue(indexExists("idx_subscriptions_stripe_customer_id"),   "missing idx_subscriptions_stripe_customer_id"),
                () -> assertTrue(indexExists("idx_subscriptions_stripe_subscription_id"), "missing idx_subscriptions_stripe_subscription_id"),
                () -> assertTrue(indexExists("idx_payment_history_subscription_id"),    "missing idx_payment_history_subscription_id"),
                () -> assertTrue(indexExists("idx_payment_history_stripe_payment_id"),  "missing idx_payment_history_stripe_payment_id")
        );
    }

    @Test
    void partialIndexOnActiveSubscriptionsExists() throws Exception {
        assertTrue(indexExists("idx_subscriptions_active"),
                "missing idx_subscriptions_active (partial index for active/trialing rows)");
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

    private boolean uniqueConstraintExists(String tableName, String columnName) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "AND tc.constraint_schema = kcu.constraint_schema " +
                        "WHERE tc.table_name = ? AND kcu.column_name = ? " +
                        "AND tc.constraint_type = 'UNIQUE'")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) >= 1;
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
