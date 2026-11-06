package com.a9.arbor.grc.tenancy;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Default target for the routing datasource. It is used only when no tenant is in context, which is
 * a programming error for tenant-scoped operations: so it fails fast with a clear message rather
 * than silently connecting to the wrong database.
 */
public class NoTenantDataSource implements DataSource {

    private SQLException fail() {
        return new SQLException("No tenant in context: a tenant-scoped operation was attempted "
                + "without an X-Tenant-ID. Ensure the request carries a valid tenant.");
    }

    @Override
    public Connection getConnection() throws SQLException {
        throw fail();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw fail();
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Cannot unwrap to " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
