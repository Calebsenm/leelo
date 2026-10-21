package com.leelo.dao;

import com.leelo.model.BookStats;
import com.leelo.model.TermStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

/** Persistence of the cached statistics of the books. */
public class BookStatsDAO {

    /** Returns the cached statistics of a book, or {@code null} when there are none. */
    public BookStats findByBook(int bookId) {
        String sql = "SELECT * FROM book_stats WHERE book_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BookStats stats = new BookStats();
                    stats.setBookId(rs.getInt("book_id"));
                    stats.setDistinctTerms(rs.getInt("distinct_terms"));
                    stats.setDistinctUnknowns(rs.getInt("distinct_unknowns"));
                    stats.setUnknownPercent(rs.getInt("unknown_percent"));
                    stats.setStatusDistribution(parseDistribution(rs.getString("status_distribution")));
                    return stats;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Stores the statistics of a book, replacing the previous values. */
    public boolean save(BookStats stats) {
        String sql = "INSERT INTO book_stats(book_id, distinct_terms, distinct_unknowns, "
                + "unknown_percent, status_distribution) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(book_id) DO UPDATE SET distinct_terms = excluded.distinct_terms, "
                + "distinct_unknowns = excluded.distinct_unknowns, "
                + "unknown_percent = excluded.unknown_percent, "
                + "status_distribution = excluded.status_distribution";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stats.getBookId());
            statement.setInt(2, stats.getDistinctTerms());
            statement.setInt(3, stats.getDistinctUnknowns());
            statement.setInt(4, stats.getUnknownPercent());
            statement.setString(5, formatDistribution(stats.getStatusDistribution()));
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Removes the cached statistics of a book, forcing them to be recalculated. */
    public boolean delete(int bookId) {
        String sql = "DELETE FROM book_stats WHERE book_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Encodes the status distribution as a compact "status:count" list. */
    private String formatDistribution(Map<TermStatus, Integer> distribution) {
        StringBuilder builder = new StringBuilder();
        for (TermStatus status : TermStatus.values()) {
            int count = distribution.getOrDefault(status, 0);
            if (count == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(status.getCode()).append(':').append(count);
        }
        return builder.toString();
    }

    /** Decodes the status distribution from its compact representation. */
    private Map<TermStatus, Integer> parseDistribution(String value) {
        Map<TermStatus, Integer> distribution = new EnumMap<>(TermStatus.class);
        if (value == null || value.isBlank()) {
            return distribution;
        }
        for (String part : value.split(",")) {
            String[] pieces = part.split(":");
            if (pieces.length != 2) {
                continue;
            }
            try {
                distribution.put(TermStatus.fromCode(Integer.parseInt(pieces[0].trim())),
                        Integer.parseInt(pieces[1].trim()));
            } catch (NumberFormatException ignored) {
                // Skip the malformed entries.
            }
        }
        return distribution;
    }
}
