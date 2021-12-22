package com.matthewcash.network;

import java.beans.PropertyVetoException;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariDataSource;

import net.md_5.bungee.api.plugin.PluginLogger;

public class DatabaseManager implements Closeable {

    static final String username = ConfigManager.getConfig().getString("database.username");
    static final String password = ConfigManager.getConfig().getString("database.password");
    static final String url = ConfigManager.getConfig().getString("database.url");

    private final HikariDataSource dataSource;

    public DatabaseManager() throws IOException, SQLException, PropertyVetoException {
        dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setPoolName("NetworkBans-Pool");

        dataSource.setMinimumIdle(5);
        dataSource.setMaximumPoolSize(30);
        dataSource.setIdleTimeout(30000);

        dataSource.setLeakDetectionThreshold(60000);

        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.addDataSourceProperty("characterEncoding", "utf-8");
        dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");

        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public void generateTable() {
        try {
            Connection connection = this.getConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS bans (uuid VARCHAR(36) NOT NULL, reason VARCHAR(1000) NOT NULL, banUntil BIGINT(19) UNSIGNED);");
            stmt.execute();
            connection.close();
        } catch (SQLException e) {
            PluginLogger.getLogger("NetworkBans").severe("Database error occurred while creating bans table");
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        this.dataSource.close();
    }
}
