package com.leelo.dao;

import com.leelo.model.Bookmark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistence of the bookmarks pointing at the pages of a book. */
public class BookmarkDAO {

    /** Returns the bookmarks of a book, ordered by the page they point at. */
    public List<Bookmark> findByBook(int bookId) {
        List<Bookmark> bookmarks = new ArrayList<>();
        String sql = "SELECT b.*, p.book_id, p.page_order FROM bookmarks b "
                + "JOIN book_pages p ON p.id = b.page_id WHERE p.book_id = ? ORDER BY p.page_order";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    bookmarks.add(readBookmark(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookmarks;
    }

    /** Inserts a bookmark and returns its id, or zero on failure. */
    public int insert(Bookmark bookmark) {
        String sql = "INSERT INTO bookmarks(page_id, title) VALUES (?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, bookmark.getPageId());
            statement.setString(2, bookmark.getTitle());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    bookmark.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Updates the title of a bookmark. */
    public boolean update(Bookmark bookmark) {
        String sql = "UPDATE bookmarks SET title = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bookmark.getTitle());
            statement.setInt(2, bookmark.getId());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Deletes a bookmark. */
    public boolean delete(int id) {
        String sql = "DELETE FROM bookmarks WHERE id = ?";
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

    private Bookmark readBookmark(ResultSet rs) throws SQLException {
        Bookmark bookmark = new Bookmark();
        bookmark.setId(rs.getInt("id"));
        bookmark.setPageId(rs.getInt("page_id"));
        bookmark.setTitle(rs.getString("title"));
        try {
            bookmark.setBookId(rs.getInt("book_id"));
            bookmark.setPageNumber(rs.getInt("page_order"));
        } catch (SQLException ignored) {
            // The columns are not part of every query.
        }
        return bookmark;
    }
}
