package com.leelo.dao;

import com.leelo.model.Book;
import com.leelo.model.BookPage;
import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.parse.ParserRegistry;
import com.leelo.service.PageSplitter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Brings the data of the earlier versions of the application into the current
 * schema.
 *
 * <p>The first versions kept the whole library in a single table of texts and
 * the vocabulary in a table of words. The data is copied once, when the new
 * tables are still empty, and the old tables are left untouched.
 */
public final class LegacyDataBridge {

    /** Name of the language the imported texts are given. */
    public static final String DEFAULT_LANGUAGE_NAME = "Default";

    /** Key of the setting that records that the copy was already made. */
    private static final String DONE_KEY = "legacy_data_imported";

    private LegacyDataBridge() {
    }

    /**
     * Copies the data of the earlier versions, when there is any and when the
     * new tables are still empty.
     *
     * <p>The whole copy runs in a single transaction, so that a failure leaves
     * the database untouched and the copy can simply be tried again later.
     */
    public static void importIfNeeded() {
        if (isDone()) {
            return;
        }
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                importTexts(connection);
                importWords(connection);
                markDone(connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** True when the copy was already made. */
    private static boolean isDone() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT value FROM app_settings WHERE key = ?")) {
            statement.setString(1, DONE_KEY);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /** Records that the copy was made. */
    private static void markDone(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO app_settings(key, key_type, value) VALUES (?, 'system', '1')")) {
            statement.setString(1, DONE_KEY);
            statement.executeUpdate();
        }
    }

    /** Copies the texts of the library into books and their pages. */
    private static void importTexts(Connection connection) throws SQLException {
        if (!isEmpty(connection, "books") || !tableExists(connection, "texts")) {
            return;
        }
        List<LegacyText> texts = readLegacyTexts(connection);
        if (texts.isEmpty()) {
            return;
        }
        Language language = findOrCreateDefaultLanguage(connection);

        for (LegacyText text : texts) {
            Book book = new Book();
            book.setTitle(text.title().isBlank() ? "Texto" : text.title());
            book.setLanguageId(language.getId());
            int bookId = insertBook(connection, book);
            if (bookId <= 0) {
                continue;
            }
            List<String> pages = PageSplitter.split(text.content(), language,
                    PageSplitter.SplitBy.PARAGRAPHS, PageSplitter.DEFAULT_THRESHOLD);
            int order = 1;
            for (String pageContent : pages) {
                BookPage page = new BookPage();
                page.setBookId(bookId);
                page.setOrder(order++);
                page.setContent(pageContent);
                page.setWordCount(countWords(pageContent, language));
                insertPage(connection, page);
            }
        }
    }

    /** Copies the words of the vocabulary into terms. */
    private static void importWords(Connection connection) throws SQLException {
        if (!isEmpty(connection, "terms") || !tableExists(connection, "words")) {
            return;
        }
        List<LegacyWord> words = readLegacyWords(connection);
        if (words.isEmpty()) {
            return;
        }
        Language language = findOrCreateDefaultLanguage(connection);

        for (LegacyWord word : words) {
            Term term = new Term();
            term.setLanguageId(language.getId());
            term.setText(word.text());
            term.setTextLc(word.text().toLowerCase(java.util.Locale.ROOT));
            term.setTokenCount(1);
            term.setStatus(mapStatus(word.state()));
            term.setTranslation(word.translation());
            term.setRomanization(word.pronunciation());
            insertTerm(connection, term);
        }
    }

    /** Reads the texts of the earlier versions. */
    private static List<LegacyText> readLegacyTexts(Connection connection) throws SQLException {
        List<LegacyText> texts = new ArrayList<>();
        String sql = "SELECT tittle, text FROM texts ORDER BY id_text";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String content = rs.getString("text");
                if (content == null || content.isBlank()) {
                    continue;
                }
                String title = rs.getString("tittle");
                texts.add(new LegacyText(title == null ? "" : title, content));
            }
        }
        return texts;
    }

    /** Reads the words of the earlier versions. */
    private static List<LegacyWord> readLegacyWords(Connection connection) throws SQLException {
        List<LegacyWord> words = new ArrayList<>();
        String sql = "SELECT term, translation, pronunciation, state FROM words";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String text = rs.getString("term");
                if (text == null || text.isBlank()) {
                    continue;
                }
                words.add(new LegacyWord(text,
                        nullToEmpty(rs.getString("translation")),
                        nullToEmpty(rs.getString("pronunciation")),
                        rs.getInt("state")));
            }
        }
        return words;
    }

    /** Maps the statuses of the earlier versions to the current ones. */
    private static TermStatus mapStatus(int legacyState) {
        return switch (legacyState) {
            case 1 -> TermStatus.NEW_1;
            case 2 -> TermStatus.LEARNING_3;
            case 3 -> TermStatus.LEARNED;
            case 4 -> TermStatus.WELL_KNOWN;
            default -> TermStatus.NEW_1;
        };
    }

    /** Returns the language the imported texts belong to, creating it when needed. */
    private static Language findOrCreateDefaultLanguage(Connection connection) throws SQLException {
        String findSql = "SELECT * FROM languages WHERE name = ? COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setString(1, DEFAULT_LANGUAGE_NAME);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Language language = new Language();
                    language.setId(rs.getInt("id"));
                    language.setName(rs.getString("name"));
                    language.setParserType(rs.getString("parser_type"));
                    return language;
                }
            }
        }

        String insertSql = "INSERT INTO languages(name, parser_type, character_substitutions, "
                + "regexp_split_sentences, exceptions_split_sentences, word_characters, "
                + "right_to_left, show_romanization) VALUES (?, ?, ?, ?, ?, ?, 0, 0)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, DEFAULT_LANGUAGE_NAME);
            statement.setString(2, com.leelo.parse.SpaceDelimitedParser.TYPE);
            statement.setString(3, Language.DEFAULT_CHARACTER_SUBSTITUTIONS);
            statement.setString(4, Language.DEFAULT_SPLIT_SENTENCES);
            statement.setString(5, Language.DEFAULT_SPLIT_EXCEPTIONS);
            statement.setString(6, Language.DEFAULT_WORD_CHARACTERS);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Language language = new Language();
                    language.setId(keys.getInt(1));
                    language.setName(DEFAULT_LANGUAGE_NAME);
                    language.setParserType(com.leelo.parse.SpaceDelimitedParser.TYPE);
                    language.setCharacterSubstitutions(Language.DEFAULT_CHARACTER_SUBSTITUTIONS);
                    language.setRegexpSplitSentences(Language.DEFAULT_SPLIT_SENTENCES);
                    language.setExceptionsSplitSentences(Language.DEFAULT_SPLIT_EXCEPTIONS);
                    language.setWordCharacters(Language.DEFAULT_WORD_CHARACTERS);
                    return language;
                }
            }
        }
        throw new SQLException("The default language could not be created");
    }

    /** True when the table holds no rows. */
    private static boolean isEmpty(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /** True when the table exists in the database. */
    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserts a book and returns its id. */
    private static int insertBook(Connection connection, Book book) throws SQLException {
        String sql = "INSERT INTO books(language_id, title, source_uri, archived, current_page_id, "
                + "creation_date) VALUES (?, ?, '', 0, 0, datetime('now'))";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, book.getLanguageId());
            statement.setString(2, book.getTitle());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** Inserts a page and returns its id. */
    private static int insertPage(Connection connection, BookPage page) throws SQLException {
        String sql = "INSERT INTO book_pages(book_id, page_order, content, word_count) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, page.getBookId());
            statement.setInt(2, page.getOrder());
            statement.setString(3, page.getContent());
            statement.setInt(4, page.getWordCount());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** Inserts a term. */
    private static void insertTerm(Connection connection, Term term) throws SQLException {
        String sql = "INSERT INTO terms(language_id, text, text_lc, status, translation, romanization, "
                + "token_count, sync_status, created_at, status_changed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, datetime('now'), datetime('now'))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, term.getLanguageId());
            statement.setString(2, term.getText());
            statement.setString(3, term.getTextLc());
            statement.setInt(4, term.getStatus().getCode());
            statement.setString(5, term.getTranslation());
            statement.setString(6, term.getRomanization());
            statement.setInt(7, term.getTokenCount());
            statement.executeUpdate();
        }
    }

    /** Counts the words of a text. */
    private static int countWords(String content, Language language) {
        int count = 0;
        for (var token : ParserRegistry.getParserOrDefault(language.getParserType())
                .getParsedTokens(content, language)) {
            if (token.isWord()) {
                count++;
            }
        }
        return count;
    }

    /** Replaces a missing value by an empty string. */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** A text of the earlier versions. */
    private record LegacyText(String title, String content) {
    }

    /** A word of the earlier versions. */
    private record LegacyWord(String text, String translation, String pronunciation, int state) {
    }
}
