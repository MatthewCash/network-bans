package com.matthewcash.network;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class BanManager {

    private static Connection getConnection() throws SQLException {
        return NetworkBans.databaseManager.getConnection();
    }

    public static Ban getBan(PlayerData playerData) throws SQLException {
        Ban ban = null;
        Connection connection = getConnection();
        PreparedStatement stmt = connection
            .prepareStatement("SELECT * from bans WHERE uuid= ?;");
        stmt.setString(1, playerData.uuid().toString());
        ResultSet results = stmt.executeQuery();

        if (!results.next()) {
            connection.close();
            return null;
        }

        String reason = results.getString("reason");
        Date banUntil = results.getLong("banUntil") != 0
            ? new Date(results.getLong("banUntil"))
            : null;

        if (banUntil != null && banUntil.compareTo(new Date()) < 0) {
            PreparedStatement removeStatement = connection
                .prepareStatement("DELETE FROM bans WHERE uuid= ?;");
            removeStatement.setString(1, playerData.uuid().toString());
            removeStatement.execute();

            return null;
        }
        ban = new Ban(playerData.uuid(), reason, banUntil);
        connection.close();
        return ban;
    }

    public static void ban(PlayerData playerData, String reason)
        throws SQLException {
        Connection connection = getConnection();
        PreparedStatement stmt = connection
            .prepareStatement("INSERT INTO bans(uuid, reason) VALUES (?, ?)");
        stmt.setString(1, playerData.uuid().toString());
        stmt.setString(2, reason);
        stmt.execute();
        connection.close();
    }

    public static void ban(PlayerData playerData, String reason, Date banUntil)
        throws SQLException {
        Connection connection = getConnection();
        PreparedStatement stmt = connection
            .prepareStatement(
                "INSERT INTO bans(uuid, reason, banUntil) VALUES (?, ?, ?)"
            );
        stmt.setString(1, playerData.uuid().toString());
        stmt.setString(2, reason);
        stmt.setLong(3, banUntil.getTime());
        stmt.execute();
        connection.close();
    }

    public static void unban(PlayerData playerData) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement stmt = connection
            .prepareStatement("DELETE FROM bans WHERE uuid= ?");
        stmt.setString(1, playerData.uuid().toString());
        stmt.execute();
        connection.close();
    }
}
