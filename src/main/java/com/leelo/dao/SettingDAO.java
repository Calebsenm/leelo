package com.leelo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistence of the settings.
 *
 * <p>Settings are stored as text; the {@code key_type} column separates the
 * settings of the user from the ones of the application itself.
 */
public class SettingDAO {

    /** Returns the value of a setting, or {@code null} when it is not set. */
    public String getValue(String key) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Stores the value of a setting, creating it when needed. */
    public void setValue(String key, String value) {
        String sql = "INSERT INTO app_settings(key, key_type, value) VALUES (?, 'user', ?) "
                + "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value == null ? "" : value);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Removes a setting. */
    public void delete(String key) {
        String sql = "DELETE FROM app_settings WHERE key = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Returns every setting of the user, keyed by its name. */
    public Map<String, String> findAllUserSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        String sql = "SELECT key, value FROM app_settings WHERE key_type = 'user' ORDER BY key";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                settings.put(rs.getString("key"), rs.getString("value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
    }

    /** True when the setting exists. */
    public boolean exists(String key) {
        String sql = "SELECT 1 FROM app_settings WHERE key = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
