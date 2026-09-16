package com.leelo.dao;

import com.leelo.model.Word;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WordDAO {
    public boolean insertWord(Word word) {
        String sql = "INSERT INTO words(term, translation, pronunciation, state, url_img) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, word.getTerm());
            pstmt.setString(2, word.getTranslation());
            pstmt.setString(3, word.getPronunciation());
            pstmt.setInt(4, word.getState());
            pstmt.setString(5, word.getUrlImg());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Word> listAll() {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT * FROM words";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Word word = createWordFromResultSet(rs);
                words.add(word);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return words;
    }
    
    public boolean updateWord(Word word) {
        String sql = "UPDATE words SET term = ?, translation = ?, pronunciation = ?, state = ?, url_img = ? WHERE id_term = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, word.getTerm());
            pstmt.setString(2, word.getTranslation());
            pstmt.setString(3, word.getPronunciation());
            pstmt.setInt(4, word.getState());
            pstmt.setString(5, word.getUrlImg());
            pstmt.setInt(6, word.getIdTerm());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating word: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteWord(int idTerm) {
        String sql = "DELETE FROM words WHERE id_term = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idTerm);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Helper method to create a Word object from a ResultSet
     * @param rs ResultSet containing word data
     * @return Word object populated with data from ResultSet
     * @throws SQLException if there's an error reading from ResultSet
     */
    private Word createWordFromResultSet(ResultSet rs) throws SQLException {
        Word word = new Word();
        word.setIdTerm(rs.getInt("id_term"));
        word.setTerm(rs.getString("term"));
        word.setTranslation(rs.getString("translation"));
        word.setPronunciation(rs.getString("pronunciation"));
        word.setState(rs.getInt("state"));
        word.setUrlImg(rs.getString("url_img"));
        return word;
    }
}