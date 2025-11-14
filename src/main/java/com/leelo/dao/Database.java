package com.leelo.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:leelo.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            //Create table text 
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS texts (" +
                    "id_text INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "tittle TEXT NOT NULL," +
                    "text TEXT NOT NULL," +
                    "creation_date TEXT NOT NULL)");
            //Create table word 
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS words (" +
                    "id_term INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "term TEXT NOT NULL," +
                    "translation TEXT," +
                    "pronunciation TEXT," +
                    "state INTEGER NOT NULL," +
                    "url_img TEXT," +
                    "last_review TEXT," +
                    "review_count INTEGER DEFAULT 0," +
                    "success_count INTEGER DEFAULT 0)");
            //Create table study_sessions for tracking study sessions
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS study_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "session_date TEXT NOT NULL," +
                    "words_reviewed INTEGER NOT NULL," +
                    "correct_answers INTEGER NOT NULL," +
                    "session_duration INTEGER NOT NULL)");
            // Create table for save the page of a text 
            stmt.executeUpdate( "CREATE TABLE IF NOT EXISTS progress (" +
                "id_progress INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_book INTEGER NOT NULL,"+
                "page_book INTEGER NOT NULL," +
                "FOREIGN KEY (id_book) references texts(id_text) )");

            System.out.println("Database initialization completed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 
