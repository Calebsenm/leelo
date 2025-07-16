package com.leelo.dao;

import com.leelo.model.StudySession;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for StudySession entities
 * Provides CRUD operations for study session data persistence
 */
public class StudySessionDAO {
    
    /**
     * Inserts a new study session into the database
     * @param session StudySession object to insert
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertStudySession(StudySession session) {
        if (session == null) {
            System.err.println("Cannot insert null study session");
            return false;
        }
        
        String sql = "INSERT INTO study_sessions(session_date, words_reviewed, correct_answers, session_duration) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, session.getSessionDate());
            pstmt.setInt(2, session.getWordsReviewed());
            pstmt.setInt(3, session.getCorrectAnswers());
            pstmt.setInt(4, session.getSessionDuration());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Get the generated ID and set it in the session object
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        session.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            
            return false;
        } catch (SQLException e) {
            System.err.println("Error inserting study session: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Retrieves study session history within a specified date range
     * @param from Start date for the range (inclusive)
     * @param to End date for the range (inclusive)
     * @return List of StudySession objects within the specified date range
     */
    public List<StudySession> getStudySessionHistory(LocalDate from, LocalDate to) {
        List<StudySession> sessions = new ArrayList<>();
        
        if (from == null || to == null) {
            System.err.println("Date parameters cannot be null");
            return sessions;
        }
        
        String sql = "SELECT * FROM study_sessions WHERE session_date >= ? AND session_date <= ? ORDER BY session_date DESC";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, from.format(DateTimeFormatter.ISO_LOCAL_DATE));
            pstmt.setString(2, to.format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StudySession session = createStudySessionFromResultSet(rs);
                sessions.add(session);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving study session history from " + from + " to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return sessions;
    }
    
    /**
     * Retrieves all study sessions from the database
     * @return List of all StudySession objects
     */
    public List<StudySession> getAllStudySessions() {
        List<StudySession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM study_sessions ORDER BY session_date DESC";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StudySession session = createStudySessionFromResultSet(rs);
                sessions.add(session);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all study sessions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return sessions;
    }
    
    /**
     * Retrieves a specific study session by its ID
     * @param id The ID of the study session to retrieve
     * @return StudySession object if found, null otherwise
     */
    public StudySession getStudySessionById(int id) {
        String sql = "SELECT * FROM study_sessions WHERE id = ?";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return createStudySessionFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving study session with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Updates an existing study session in the database
     * @param session StudySession object with updated data
     * @return true if update was successful, false otherwise
     */
    public boolean updateStudySession(StudySession session) {
        if (session == null) {
            System.err.println("Cannot update null study session");
            return false;
        }
        
        String sql = "UPDATE study_sessions SET session_date = ?, words_reviewed = ?, correct_answers = ?, session_duration = ? WHERE id = ?";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, session.getSessionDate());
            pstmt.setInt(2, session.getWordsReviewed());
            pstmt.setInt(3, session.getCorrectAnswers());
            pstmt.setInt(4, session.getSessionDuration());
            pstmt.setInt(5, session.getId());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating study session with ID " + session.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Deletes a study session from the database
     * @param id The ID of the study session to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteStudySession(int id) {
        String sql = "DELETE FROM study_sessions WHERE id = ?";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting study session with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Retrieves study sessions for a specific date
     * @param date The date to retrieve sessions for
     * @return List of StudySession objects for the specified date
     */
    public List<StudySession> getStudySessionsByDate(LocalDate date) {
        List<StudySession> sessions = new ArrayList<>();
        
        if (date == null) {
            System.err.println("Date parameter cannot be null");
            return sessions;
        }
        
        String sql = "SELECT * FROM study_sessions WHERE session_date = ? ORDER BY id DESC";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StudySession session = createStudySessionFromResultSet(rs);
                sessions.add(session);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving study sessions for date " + date + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return sessions;
    }
    
    /**
     * Gets the total count of study sessions in the database
     * @return Total number of study sessions
     */
    public int getTotalSessionCount() {
        String sql = "SELECT COUNT(*) as session_count FROM study_sessions";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("session_count");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total session count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Gets the most recent study session
     * @return Most recent StudySession object, or null if no sessions exist
     */
    public StudySession getMostRecentSession() {
        String sql = "SELECT * FROM study_sessions ORDER BY session_date DESC, id DESC LIMIT 1";
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return createStudySessionFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving most recent session: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Helper method to create a StudySession object from a ResultSet
     * @param rs ResultSet containing study session data
     * @return StudySession object populated with data from ResultSet
     * @throws SQLException if there's an error reading from ResultSet
     */
    private StudySession createStudySessionFromResultSet(ResultSet rs) throws SQLException {
        StudySession session = new StudySession();
        session.setId(rs.getInt("id"));
        session.setSessionDate(rs.getString("session_date"));
        session.setWordsReviewed(rs.getInt("words_reviewed"));
        session.setCorrectAnswers(rs.getInt("correct_answers"));
        session.setSessionDuration(rs.getInt("session_duration"));
        return session;
    }
}