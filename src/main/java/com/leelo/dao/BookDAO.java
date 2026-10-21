package com.leelo.dao;

import com.leelo.model.Book;
import com.leelo.model.BookTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistence of the books and their tags. */
public class BookDAO {

    /** Returns the books of a language, or every book when the language is zero. */
    public List<Book> findAll(int languageId, boolean includeArchived) {
        List<Book> books = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT b.*, l.name AS language_name, "
                        + "(SELECT COUNT(*) FROM book_pages p WHERE p.book_id = b.id) AS page_count, "
                        + "(SELECT p.page_order FROM book_pages p WHERE p.id = b.current_page_id) "
                        + "AS current_page "
                        + "FROM books b JOIN languages l ON l.id = b.language_id WHERE 1 = 1");
        if (languageId > 0) {
            sql.append(" AND b.language_id = ?");
        }
        if (!includeArchived) {
            sql.append(" AND b.archived = 0");
        }
        sql.append(" ORDER BY b.creation_date DESC, b.id DESC");

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (languageId > 0) {
                statement.setInt(parameter++, languageId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    books.add(readBook(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (Book book : books) {
            book.setTags(findTags(book.getId()));
        }
        return books;
    }

    /** Returns the book with the given id, or {@code null}. */
    public Book findById(int id) {
        String sql = "SELECT b.*, l.name AS language_name, "
                + "(SELECT COUNT(*) FROM book_pages p WHERE p.book_id = b.id) AS page_count, "
                + "(SELECT p.page_order FROM book_pages p WHERE p.id = b.current_page_id) AS current_page "
                + "FROM books b JOIN languages l ON l.id = b.language_id WHERE b.id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Book book = readBook(rs);
                    book.setTags(findTags(book.getId()));
                    return book;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Returns the book with the given title in the given language, or {@code null}. */
    public Book findByTitle(String title, int languageId) {
        String sql = "SELECT b.*, l.name AS language_name FROM books b "
                + "JOIN languages l ON l.id = b.language_id "
                + "WHERE b.title = ? AND b.language_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setInt(2, languageId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return readBook(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Inserts a book and returns its id, or zero on failure. */
    public int insert(Book book) {
        String sql = "INSERT INTO books(language_id, title, source_uri, archived, current_page_id, "
                + "creation_date) VALUES (?, ?, ?, ?, ?, datetime('now'))";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, book.getLanguageId());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getSourceUri());
            statement.setInt(4, book.isArchived() ? 1 : 0);
            statement.setInt(5, book.getCurrentPageId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    book.setId(id);
                    saveTags(book);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Updates the metadata of a book. */
    public boolean update(Book book) {
        String sql = "UPDATE books SET language_id = ?, title = ?, source_uri = ?, archived = ?, "
                + "current_page_id = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, book.getLanguageId());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getSourceUri());
            statement.setInt(4, book.isArchived() ? 1 : 0);
            statement.setInt(5, book.getCurrentPageId());
            statement.setInt(6, book.getId());
            statement.executeUpdate();
            saveTags(book);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Deletes a book, its pages and its tag links. */
    public boolean delete(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
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

    /** Archives or unarchives a book. */
    public boolean setArchived(int bookId, boolean archived) {
        String sql = "UPDATE books SET archived = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, archived ? 1 : 0);
            statement.setInt(2, bookId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Records the page the reader was last on. */
    public boolean setCurrentPage(int bookId, int pageId) {
        String sql = "UPDATE books SET current_page_id = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pageId);
            statement.setInt(2, bookId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Returns the tags of a book. */
    public List<BookTag> findTags(int bookId) {
        List<BookTag> tags = new ArrayList<>();
        String sql = "SELECT t.* FROM book_tags t "
                + "JOIN book_tag_links l ON l.tag_id = t.id "
                + "WHERE l.book_id = ? ORDER BY t.text COLLATE NOCASE";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    BookTag tag = new BookTag();
                    tag.setId(rs.getInt("id"));
                    tag.setText(rs.getString("text"));
                    tag.setComment(rs.getString("comment"));
                    tags.add(tag);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }

    /** Replaces the tags of a book with the given ones. */
    private void saveTags(Book book) {
        String deleteSql = "DELETE FROM book_tag_links WHERE book_id = ?";
        String insertSql = "INSERT INTO book_tag_links(book_id, tag_id) VALUES (?, ?)";
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setInt(1, book.getId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                for (BookTag tag : book.getTags()) {
                    statement.setInt(1, book.getId());
                    statement.setInt(2, tag.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Book readBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("id"));
        book.setLanguageId(rs.getInt("language_id"));
        book.setLanguageName(rs.getString("language_name"));
        book.setTitle(rs.getString("title"));
        book.setSourceUri(rs.getString("source_uri"));
        book.setArchived(rs.getInt("archived") != 0);
        book.setCurrentPageId(rs.getInt("current_page_id"));
        book.setCreationDate(rs.getString("creation_date"));
        try {
            book.setPageCount(rs.getInt("page_count"));
        } catch (SQLException ignored) {
            // The column is not part of every query.
        }
        try {
            book.setCurrentPageNumber(rs.getInt("current_page"));
        } catch (SQLException ignored) {
            // The column is not part of every query.
        }
        return book;
    }
}
