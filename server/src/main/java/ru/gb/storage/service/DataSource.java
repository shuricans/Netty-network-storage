package ru.gb.storage.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DataSource {

    private final static HikariDataSource ds;

    static {
        Properties properties = ConfigProperties.getHikariProperties();
        final HikariConfig config = new HikariConfig(properties);
        ds = new HikariDataSource(config);
    }

    private DataSource() {
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
