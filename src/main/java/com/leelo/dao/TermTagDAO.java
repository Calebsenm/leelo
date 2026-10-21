package com.leelo.dao;

import com.leelo.model.TermTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistence of the tags that can be attached to a term. */
public class TermTagDAO {

    /** Returns every term tag, with the number of terms carrying it. */
    public List<TermTag> findAll() {
        List<TermTag> tags = new ArrayList<>();
        String sql = "SELECT t.*, (SELECT COUNT(*) FROM term_tag_links l WHERE l.tag_id = t.id) "
                + "AS term_count FROM term_tags t ORDER BY t.text COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                TermTag tag = new TermTag();
                tag.setId(rs.getInt("id"));
                tag.setText(rs.getString("text"));
                tag.setComment(rs.getString("comment"));
                tag.setTermCount(rs.getInt("term_count"));
                tags.add(tag);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }

    /** Returns the tag with the given id, or {@code null}. */
    public TermTag findById(int id) {
        String sql = "SELECT * FROM term_tags WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    TermTag tag = new TermTag();
                    tag.setId(rs.getInt("id"));
                    tag.setText(rs.getString("text"));
                    tag.setComment(rs.getString("comment"));
                    return tag;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the tag with the given text, creating it when it does not exist.
     */
    public TermTag findOrCreateByText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String findSql = "SELECT * FROM term_tags WHERE text = ? COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setString(1, trimmed);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    TermTag tag = new TermTag();
                    tag.setId(rs.getInt("id"));
                    tag.setText(rs.getString("text"));
                    tag.setComment(rs.getString("comment"));
                    return tag;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String insertSql = "INSERT INTO term_tags(text, comment) VALUES (?, '')";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, trimmed);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    TermTag tag = new TermTag();
                    tag.setId(keys.getInt(1));
                    tag.setText(trimmed);
                    return tag;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Updates the text and the comment of a tag. */
    public boolean update(TermTag tag) {
        String sql = "UPDATE term_tags SET text = ?, comment = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tag.getText());
            statement.setString(2, tag.getComment());
            statement.setInt(3, tag.getId());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Deletes a tag and removes it from every term. */
    public boolean delete(int id) {
        String sql = "DELETE FROM term_tags WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
