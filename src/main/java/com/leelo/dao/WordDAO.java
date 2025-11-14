package com.leelo.dao;

import com.leelo.model.Word;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        String sql = "UPDATE words SET term = ?, translation = ?, pronunciation = ?, state = ?, url_img = ?, " +
                    "last_review = ?, review_count = ?, success_count = ? WHERE id_term = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, word.getTerm());
            pstmt.setString(2, word.getTranslation());
            pstmt.setString(3, word.getPronunciation());
            pstmt.setInt(4, word.getState());
            pstmt.setString(5, word.getUrlImg());
            pstmt.setString(6, word.getLastReview());
            pstmt.setInt(7, word.getReviewCount());
            pstmt.setInt(8, word.getSuccessCount());
            pstmt.setInt(9, word.getIdTerm());
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
        
        // Set spaced repetition fields (may be null for older records)
        word.setLastReview(rs.getString("last_review"));
        word.setReviewCount(rs.getInt("review_count"));
        word.setSuccessCount(rs.getInt("success_count"));
        
        return word;
    }
    
    /**
     * Retrieves all words that are due for review based on their last review date and current state
     * @return List of words that need to be reviewed
     */
    public List<Word> getWordsDueForReview() {
        List<Word> dueWords = new ArrayList<>();
        String sql = "SELECT * FROM words";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            while (rs.next()) {
                Word word = createWordFromResultSet(rs);
                
                // Check if word is due for review
                if (word.isDueForReview()) {
                    dueWords.add(word);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving words due for review: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dueWords;
    }
    
    /**
     * Retrieves words filtered by their learning state
     * @param state The learning state to filter by (0-5)
     * @return List of words in the specified state
     */
    public List<Word> getWordsByState(int state) {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT * FROM words WHERE state = ?";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, state);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Word word = createWordFromResultSet(rs);
                words.add(word);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving words by state " + state + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return words;
    }
    
    /**
     * Updates only the spaced repetition data for a word (last_review, review_count, success_count)
     * @param word Word object with updated review data
     * @return true if update was successful, false otherwise
     */
    public boolean updateWordReviewData(Word word) {
        String sql = "UPDATE words SET last_review = ?, review_count = ?, success_count = ?, state = ? WHERE id_term = ?";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, word.getLastReview());
            pstmt.setInt(2, word.getReviewCount());
            pstmt.setInt(3, word.getSuccessCount());
            pstmt.setInt(4, word.getState());
            pstmt.setInt(5, word.getIdTerm());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating word review data for word ID " + word.getIdTerm() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Updates review data for multiple words in a batch operation for better performance
     * @param words List of words with updated review data
     * @return true if all updates were successful, false if any failed
     */
    public boolean updateWordReviewDataBatch(List<Word> words) {
        if (words == null || words.isEmpty()) {
            return true;
        }
        
        String sql = "UPDATE words SET last_review = ?, review_count = ?, success_count = ?, state = ? WHERE id_term = ?";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); // Start transaction
            
            for (Word word : words) {
                pstmt.setString(1, word.getLastReview());
                pstmt.setInt(2, word.getReviewCount());
                pstmt.setInt(3, word.getSuccessCount());
                pstmt.setInt(4, word.getState());
                pstmt.setInt(5, word.getIdTerm());
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            conn.commit(); // Commit transaction
            
            // Check if all updates were successful
            for (int result : results) {
                if (result == Statement.EXECUTE_FAILED) {
                    return false;
                }
            }
            
            return true;
        } catch (SQLException e) {
            System.err.println("Error in batch update of word review data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets the total count of words in the database
     * @return Total number of words
     */
    public int getTotalWordCount() {
        String sql = "SELECT COUNT(*) as total_count FROM words";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_count");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total word count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
} 