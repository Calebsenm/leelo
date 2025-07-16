package com.leelo.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a study session for tracking vocabulary review sessions
 */
public class StudySession {
    private int id;
    private String sessionDate;
    private int wordsReviewed;
    private int correctAnswers;
    private int sessionDuration; // Duration in minutes
    private List<Word> reviewedWords;
    
    /**
     * Default constructor
     */
    public StudySession() {
        this.reviewedWords = new ArrayList<>();
        this.sessionDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * Constructor with all fields
     * @param id Session ID
     * @param sessionDate Date of the session (ISO format string)
     * @param wordsReviewed Number of words reviewed in this session
     * @param correctAnswers Number of correct answers in this session
     * @param sessionDuration Duration of the session in minutes
     */
    public StudySession(int id, String sessionDate, int wordsReviewed, int correctAnswers, int sessionDuration) {
        this.id = id;
        this.sessionDate = sessionDate;
        this.wordsReviewed = wordsReviewed;
        this.correctAnswers = correctAnswers;
        this.sessionDuration = sessionDuration;
        this.reviewedWords = new ArrayList<>();
    }
    
    /**
     * Constructor for creating a new session (without ID)
     * @param sessionDate Date of the session (ISO format string)
     * @param wordsReviewed Number of words reviewed in this session
     * @param correctAnswers Number of correct answers in this session
     * @param sessionDuration Duration of the session in minutes
     */
    public StudySession(String sessionDate, int wordsReviewed, int correctAnswers, int sessionDuration) {
        this.sessionDate = sessionDate;
        this.wordsReviewed = wordsReviewed;
        this.correctAnswers = correctAnswers;
        this.sessionDuration = sessionDuration;
        this.reviewedWords = new ArrayList<>();
    }
    
    // Getters and Setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSessionDate() {
        return sessionDate;
    }
    
    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }
    
    public int getWordsReviewed() {
        return wordsReviewed;
    }
    
    public void setWordsReviewed(int wordsReviewed) {
        this.wordsReviewed = wordsReviewed;
    }
    
    public int getCorrectAnswers() {
        return correctAnswers;
    }
    
    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
    
    public int getSessionDuration() {
        return sessionDuration;
    }
    
    public void setSessionDuration(int sessionDuration) {
        this.sessionDuration = sessionDuration;
    }
    
    public List<Word> getReviewedWords() {
        return reviewedWords;
    }
    
    public void setReviewedWords(List<Word> reviewedWords) {
        this.reviewedWords = reviewedWords != null ? reviewedWords : new ArrayList<>();
    }
    
    // Helper methods for calculating accuracy and session statistics
    
    /**
     * Calculates the accuracy rate for this study session
     * @return accuracy rate as a percentage (0.0 to 1.0)
     */
    public double getAccuracy() {
        if (wordsReviewed == 0) {
            return 0.0;
        }
        return (double) correctAnswers / wordsReviewed;
    }
    
    /**
     * Calculates the accuracy rate as a percentage
     * @return accuracy rate as a percentage (0 to 100)
     */
    public double getAccuracyPercentage() {
        return getAccuracy() * 100.0;
    }
    
    /**
     * Gets the number of incorrect answers in this session
     * @return number of incorrect answers
     */
    public int getIncorrectAnswers() {
        return wordsReviewed - correctAnswers;
    }
    
    /**
     * Checks if this session has any reviewed words
     * @return true if words were reviewed, false otherwise
     */
    public boolean hasReviewedWords() {
        return wordsReviewed > 0;
    }
    
    /**
     * Adds a word to the list of reviewed words in this session
     * @param word The word that was reviewed
     */
    public void addReviewedWord(Word word) {
        if (reviewedWords == null) {
            reviewedWords = new ArrayList<>();
        }
        reviewedWords.add(word);
    }
    
    /**
     * Gets the session date as a LocalDate object
     * @return LocalDate representation of the session date
     */
    public LocalDate getSessionDateAsLocalDate() {
        try {
            return LocalDate.parse(sessionDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
    
    @Override
    public String toString() {
        return "StudySession{" +
                "id=" + id +
                ", sessionDate='" + sessionDate + '\'' +
                ", wordsReviewed=" + wordsReviewed +
                ", correctAnswers=" + correctAnswers +
                ", sessionDuration=" + sessionDuration +
                ", accuracy=" + String.format("%.1f", getAccuracyPercentage()) + "%" +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StudySession that = (StudySession) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}