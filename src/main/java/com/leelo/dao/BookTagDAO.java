package com.leelo.dao;

import com.leelo.model.BookTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistence of the tags that can be attached to a book. */
public class BookTagDAO {

    /** Returns every book tag, with the number of books carrying it. */
    public List<BookTag> findAll() {
        List<BookTag> tags = new ArrayList<>();
        String sql = "SELECT t.*, (SELECT COUNT(*) FROM book_tag_links l WHERE l.tag_id = t.id) "
                + "AS book_count FROM book_tags t ORDER BY t.text COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                BookTag tag = new BookTag();
                tag.setId(rs.getInt("id"));
                tag.setText(rs.getString("text"));
                tag.setComment(rs.getString("comment"));
                tags.add(tag);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }

    /** Returns the tag with the given text, creating it when it does not exist. */
    public BookTag findOrCreateByText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String findSql = "SELECT * FROM book_tags WHERE text = ? COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setString(1, trimmed);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BookTag tag = new BookTag();
                    tag.setId(rs.getInt("id"));
                    tag.setText(rs.getString("text"));
                    tag.setComment(rs.getString("comment"));
                    return tag;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String insertSql = "INSERT INTO book_tags(text, comment) VALUES (?, '')";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, trimmed);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    BookTag tag = new BookTag();
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
    public boolean update(BookTag tag) {
        String sql = "UPDATE book_tags SET text = ?, comment = ? WHERE id = ?";
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

    /** Deletes a tag and removes it from every book. */
    public boolean delete(int id) {
        String sql = "DELETE FROM book_tags WHERE id = ?";
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
