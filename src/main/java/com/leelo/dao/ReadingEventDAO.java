package com.leelo.dao;

import com.leelo.model.ReadingEvent;
import com.leelo.util.Timestamps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence of the reading events, which feed the statistics. */
public class ReadingEventDAO {

    /** Records that a page was read. */
    public int insert(ReadingEvent event) {
        String sql = "INSERT INTO reading_events(language_id, page_id, read_date, word_count) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, event.getLanguageId());
            statement.setInt(2, event.getPageId());
            statement.setString(3, Timestamps.format(event.getReadDate() == null ? Instant.now() : event.getReadDate()));
            statement.setInt(4, event.getWordCount());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Returns the number of words read per language and per day.
     *
     * @return the number of words read, keyed by language name and then by date
     */
    public Map<String, Map<LocalDate, Integer>> countWordsByLanguageAndDate() {
        Map<String, Map<LocalDate, Integer>> result = new LinkedHashMap<>();
        String sql = "SELECT l.name AS language_name, e.read_date, SUM(e.word_count) AS word_count "
                + "FROM reading_events e JOIN languages l ON l.id = e.language_id "
                + "GROUP BY l.name, e.read_date ORDER BY e.read_date";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String language = rs.getString("language_name");
                Instant readDate = Timestamps.parse(rs.getString("read_date"));
                if (readDate == null) {
                    continue;
                }
                LocalDate date = readDate.atZone(ZoneOffset.UTC).toLocalDate();
                result.computeIfAbsent(language, key -> new LinkedHashMap<>())
                        .merge(date, rs.getInt("word_count"), Integer::sum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Returns the days on which something was read, most recent first. */
    public List<LocalDate> findReadDays() {
        List<LocalDate> days = new ArrayList<>();
        String sql = "SELECT DISTINCT read_date FROM reading_events ORDER BY read_date DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Instant readDate = Timestamps.parse(rs.getString("read_date"));
                if (readDate != null) {
                    days.add(readDate.atZone(ZoneOffset.UTC).toLocalDate());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return days;
    }
}
