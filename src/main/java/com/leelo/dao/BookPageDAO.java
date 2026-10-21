package com.leelo.dao;

import com.leelo.model.BookPage;
import com.leelo.util.Timestamps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Persistence of the pages of the books. */
public class BookPageDAO {

    /** Returns the pages of a book, in reading order. */
    public List<BookPage> findByBook(int bookId) {
        List<BookPage> pages = new ArrayList<>();
        String sql = "SELECT * FROM book_pages WHERE book_id = ? ORDER BY page_order";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    pages.add(readPage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pages;
    }

    /** Returns the page with the given id, or {@code null}. */
    public BookPage findById(int id) {
        String sql = "SELECT * FROM book_pages WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return readPage(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Inserts a page and returns its id, or zero on failure. */
    public int insert(BookPage page) {
        String sql = "INSERT INTO book_pages(book_id, page_order, content, read_date, start_date, "
                + "word_count) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, page.getBookId());
            statement.setInt(2, page.getOrder());
            statement.setString(3, page.getContent());
            statement.setString(4, Timestamps.format(page.getReadDate()));
            statement.setString(5, Timestamps.format(page.getStartDate()));
            statement.setInt(6, page.getWordCount());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    page.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Updates the content and the word count of a page. */
    public boolean updateContent(int pageId, String content, int wordCount) {
        String sql = "UPDATE book_pages SET content = ?, word_count = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, content);
            statement.setInt(2, wordCount);
            statement.setInt(3, pageId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Marks a page as read, or as not read when the date is {@code null}. */
    public boolean setReadDate(int pageId, Instant readDate) {
        String sql = "UPDATE book_pages SET read_date = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Timestamps.format(readDate));
            statement.setInt(2, pageId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Records when a page was opened. */
    public boolean setStartDate(int pageId, Instant startDate) {
        String sql = "UPDATE book_pages SET start_date = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Timestamps.format(startDate));
            statement.setInt(2, pageId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Deletes a page. */
    public boolean delete(int pageId) {
        String sql = "DELETE FROM book_pages WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pageId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Rewrites the order of the pages of a book. */
    public boolean reorder(int bookId, List<Integer> pageIds) {
        String sql = "UPDATE book_pages SET page_order = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int order = 1;
            for (Integer pageId : pageIds) {
                statement.setInt(1, order++);
                statement.setInt(2, pageId);
                statement.addBatch();
            }
            statement.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Returns the number of pages of a book. */
    public int countByBook(int bookId) {
        String sql = "SELECT COUNT(*) FROM book_pages WHERE book_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private BookPage readPage(ResultSet rs) throws SQLException {
        BookPage page = new BookPage();
        page.setId(rs.getInt("id"));
        page.setBookId(rs.getInt("book_id"));
        page.setOrder(rs.getInt("page_order"));
        page.setContent(rs.getString("content"));
        page.setReadDate(Timestamps.parse(rs.getString("read_date")));
        page.setStartDate(Timestamps.parse(rs.getString("start_date")));
        page.setWordCount(rs.getInt("word_count"));
        return page;
    }
}
