package com.a9.arbor.grc.tenancy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates a physical SQL Server / Azure SQL database (idempotently) by connecting to {@code master}.
 *
 * <p>On Azure SQL elastic pools, pass the pool name so {@code CREATE DATABASE} uses
 * {@code SERVICE_OBJECTIVE = ELASTIC_POOL}. Without a pool name, uses a plain
 * {@code CREATE DATABASE} (local SQL Server / standalone Azure SKU).
 */
public final class SqlServerDatabaseCreator {

    /** Database names are restricted to a safe identifier set (defence against injection). */
    private static final Pattern SAFE_DB_NAME = Pattern.compile("[A-Za-z0-9_]{1,120}");

    /** Elastic pool names may include hyphens (Azure naming). */
    private static final Pattern SAFE_POOL_NAME = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private static final Set<String> BACKUP_REDUNDANCY = Set.of("LOCAL", "ZONE", "GEO", "GEOZONE");

    private SqlServerDatabaseCreator() {
    }

    public static void createIfNotExists(String masterUrl, String username, String password, String databaseName) {
        createIfNotExists(masterUrl, username, password, databaseName, null, null);
    }

    public static void createIfNotExists(
            String masterUrl,
            String username,
            String password,
            String databaseName,
            String elasticPoolName) {
        createIfNotExists(masterUrl, username, password, databaseName, elasticPoolName, null);
    }

    /**
     * @param elasticPoolName Azure SQL elastic pool to join, or {@code null}/blank for plain create
     * @param backupStorageRedundancy {@code LOCAL} / {@code ZONE} / {@code GEO} / {@code GEOZONE},
     *     or {@code null}/blank to leave the Azure default
     */
    public static void createIfNotExists(
            String masterUrl,
            String username,
            String password,
            String databaseName,
            String elasticPoolName,
            String backupStorageRedundancy) {
        String sql = createStatement(databaseName, elasticPoolName, backupStorageRedundancy);
        try (Connection connection = DriverManager.getConnection(masterUrl, username, password);
             Statement statement = connection.createStatement()) {
            if (databaseExists(statement, databaseName)) {
                return;
            }
            statement.execute(sql);
        } catch (SQLException e) {
            if (isAlreadyExists(e)) {
                return;
            }
            throw new IllegalStateException("Failed to create database " + databaseName, e);
        }
    }

    static String createStatement(String databaseName, String elasticPoolName, String backupStorageRedundancy) {
        if (databaseName == null || !SAFE_DB_NAME.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("Unsafe database name: " + databaseName);
        }
        String pool = elasticPoolName == null ? "" : elasticPoolName.trim();
        if (!pool.isEmpty() && !SAFE_POOL_NAME.matcher(pool).matches()) {
            throw new IllegalArgumentException("Unsafe elastic pool name: " + elasticPoolName);
        }
        String redundancy = backupStorageRedundancy == null
                ? ""
                : backupStorageRedundancy.trim().toUpperCase(Locale.ROOT);
        if (!redundancy.isEmpty() && !BACKUP_REDUNDANCY.contains(redundancy)) {
            throw new IllegalArgumentException("Unsupported backup storage redundancy: " + backupStorageRedundancy);
        }
        StringBuilder sql = new StringBuilder("CREATE DATABASE [").append(databaseName).append(']');
        if (!pool.isEmpty()) {
            sql.append(" ( SERVICE_OBJECTIVE = ELASTIC_POOL ( name = [").append(pool).append("] ) )");
        }
        if (!redundancy.isEmpty()) {
            sql.append(" WITH BACKUP_STORAGE_REDUNDANCY = '").append(redundancy).append('\'');
        }
        return sql.toString();
    }

    private static boolean databaseExists(Statement statement, String databaseName) throws SQLException {
        // Parameterised check — name already validated against SAFE_DB_NAME.
        String sql = "SELECT 1 FROM sys.databases WHERE name = N'" + databaseName + "'";
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }

    private static boolean isAlreadyExists(SQLException e) {
        // SQL Server / Azure SQL: 1801 database already exists; also match message for pooled drivers.
        if (e.getErrorCode() == 1801) {
            return true;
        }
        String msg = e.getMessage();
        return msg != null && msg.toLowerCase().contains("already exists");
    }
}
