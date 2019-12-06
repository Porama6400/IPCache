package com.Porama6400.IPCache.server.net;

import com.Porama6400.IPCache.server.Config;
import com.Porama6400.IPCache.server.IPCacheServerCore;
import com.Porama6400.IPCache.server.apichecker.APIResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLAdapter {
    private final IPCacheServerCore core;
    private final Config config;
    public HikariDataSource dataSource;
    public Connection con;

    public MySQLAdapter(IPCacheServerCore core, Config config) {
        this.core = core;
        this.config = config;
    }

    public void init() throws SQLException {
        HikariConfig hkconfig = new HikariConfig();
        hkconfig.setJdbcUrl(config.databaseURL);
        hkconfig.setUsername(config.databaseUser);
        hkconfig.setPassword(config.databasePass);
        hkconfig.setMaximumPoolSize(config.databasePoolSize);
        hkconfig.setConnectionTimeout(300);
        hkconfig.setValidationTimeout(300);

        dataSource = new HikariDataSource(hkconfig);
        con = dataSource.getConnection();
    }

    public void close() {
        if (!dataSource.isClosed())
            dataSource.close();
    }

    public int spread() throws SQLException {
        PreparedStatement cst = con.prepareStatement("UPDATE " + config.databaseTable
                + " SET date = (now() - INTERVAL ROUND(RAND() * 60 * 24 * " + config.databaseCacheTime + ") MINUTE);");
        cst.execute();
        return cst.getUpdateCount();
    }

    // Count number of all rows
    public int countRow() throws SQLException {
        return count("SELECT COUNT(*) FROM ipcache.ipdata;");

    }

    // Count number of rows that is VPN
    public int countVPN() throws SQLException {
        return count("SELECT COUNT(*) FROM ipcache.ipdata WHERE vpn = 1;");
    }

    private int count(String sql) throws SQLException {
        PreparedStatement statement = con.prepareStatement(sql);
        statement.execute();
        ResultSet result = statement.getResultSet();
        result.next();
        return result.getInt(1);
    }

    public void updateDatabase(String IP, APIResult result) throws SQLException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    String sqlCommand =
                            "INSERT " + config.databaseTable + " (`ip`, `vpn`, `type`, `isp`, `asn`, `country`) VALUES (?, ?, ?, ?, ?, ?)" +
                                    "ON DUPLICATE KEY UPDATE vpn = VALUES(vpn), type = VALUES(type), isp = VALUES(isp), asn = VALUES(asn), country= VALUES(country);";


                    PreparedStatement statement = con.prepareStatement(sqlCommand);
                    statement.setString(1, IP);
                    statement.setBoolean(2, result.isVPN());
                    statement.setString(3, result.getClientType());
                    statement.setString(4, result.getISP());
                    statement.setString(5, result.getASN());
                    statement.setString(6, result.getCountry());
                    statement.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        if (config.enableDatabaseUpdate) core.databaseUpdateThread.statements.add(r); //Pass to another thread
    }

    public int clean() throws SQLException {
        PreparedStatement statement = con.prepareStatement(
                "DELETE FROM " + config.databaseTable + " WHERE date < now() - INTERVAL " + config.databaseCacheTime + " DAY;");
        statement.execute();
        return statement.getUpdateCount();
    }

    public boolean allowUser(String ip) throws SQLException {
        PreparedStatement statement = con.prepareStatement("UPDATE " + config.databaseTable + " SET `vpn` = '0' WHERE `ip` = ?");
        statement.setString(1, ip);
        statement.execute();
        return statement.getUpdateCount() > 0;
    }
}
