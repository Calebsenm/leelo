package com.leelo.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the connection to the database and the definition of its schema.
 *
 * <p>The database lives next to the other application data, so that it is not
 * affected by the installation directory.
 */
public final class Database {

    private static final String DB_URL;

    static {
        String url;
        try {
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null || appData.isEmpty()) {
                appData = System.getProperty("user.home") + "\\AppData\\Local";
            }
            Path appFolder = Paths.get(appData, "Leelo");
            Files.createDirectories(appFolder);
            Path dbPath = appFolder.resolve("leelo.db");
            url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            System.out.println("Base de datos en: " + dbPath);
        } catch (Exception e) {
            e.printStackTrace();
            url = "jdbc:sqlite:leelo.db";
        }
        DB_URL = url;
    }

    private Database() {
    }

    /** Returns a new connection to the database. */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    /**
     * Creates the schema when it is missing.
     *
     * <p>The schema is created directly, without a migration step: every
     * statement is idempotent, so running it on an existing database only
     * adds what is missing.
     */
    public static void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA) {
                statement.executeUpdate(sql);
            }
            System.out.println("Database initialization completed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** The tables making up the application schema. */
    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS languages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                parser_type TEXT NOT NULL DEFAULT 'spacedel',
                character_substitutions TEXT NOT NULL DEFAULT '',
                regexp_split_sentences TEXT NOT NULL DEFAULT '',
                exceptions_split_sentences TEXT NOT NULL DEFAULT '',
                word_characters TEXT NOT NULL DEFAULT '',
                right_to_left INTEGER NOT NULL DEFAULT 0,
                show_romanization INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS language_dicts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                language_id INTEGER NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
                use_for TEXT NOT NULL,
                dict_type TEXT NOT NULL,
                dict_uri TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                language_id INTEGER NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
                title TEXT NOT NULL,
                source_uri TEXT NOT NULL DEFAULT '',
                archived INTEGER NOT NULL DEFAULT 0,
                current_page_id INTEGER NOT NULL DEFAULT 0,
                creation_date TEXT NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS book_pages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL REFERENCES books(id) ON DELETE CASCADE,
                page_order INTEGER NOT NULL,
                content TEXT NOT NULL,
                read_date TEXT,
                start_date TEXT,
                word_count INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS book_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL UNIQUE,
                comment TEXT NOT NULL DEFAULT ''
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS book_tag_links (
                book_id INTEGER NOT NULL REFERENCES books(id) ON DELETE CASCADE,
                tag_id INTEGER NOT NULL REFERENCES book_tags(id) ON DELETE CASCADE,
                PRIMARY KEY (book_id, tag_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS terms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                language_id INTEGER NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
                text TEXT NOT NULL,
                text_lc TEXT NOT NULL,
                status INTEGER NOT NULL DEFAULT 0,
                translation TEXT NOT NULL DEFAULT '',
                romanization TEXT NOT NULL DEFAULT '',
                token_count INTEGER NOT NULL DEFAULT 1,
                sync_status INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                status_changed_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS term_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL UNIQUE,
                comment TEXT NOT NULL DEFAULT ''
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS term_tag_links (
                term_id INTEGER NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
                tag_id INTEGER NOT NULL REFERENCES term_tags(id) ON DELETE CASCADE,
                PRIMARY KEY (term_id, tag_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS term_parents (
                term_id INTEGER NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
                parent_id INTEGER NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
                PRIMARY KEY (term_id, parent_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS term_images (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                term_id INTEGER NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
                source TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS term_flash_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                term_id INTEGER NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
                message TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                page_id INTEGER NOT NULL REFERENCES book_pages(id) ON DELETE CASCADE,
                title TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reading_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                language_id INTEGER NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
                page_id INTEGER,
                read_date TEXT NOT NULL DEFAULT (datetime('now')),
                word_count INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS book_stats (
                book_id INTEGER PRIMARY KEY REFERENCES books(id) ON DELETE CASCADE,
                distinct_terms INTEGER NOT NULL DEFAULT 0,
                distinct_unknowns INTEGER NOT NULL DEFAULT 0,
                unknown_percent INTEGER NOT NULL DEFAULT 0,
                status_distribution TEXT NOT NULL DEFAULT ''
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                key_type TEXT NOT NULL DEFAULT 'user',
                value TEXT NOT NULL DEFAULT ''
            )
            """
    };
}
