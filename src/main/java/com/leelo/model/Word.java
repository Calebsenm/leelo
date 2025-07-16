package com.leelo.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Word {
    private int idTerm;
    private String term;
    private String translation;
    private String pronunciation;
    private int state;
    private String urlImg;
    
    // Spaced repetition fields
    private String lastReview;
    private int reviewCount;
    private int successCount;
    
    // Spaced repetition intervals for each state (in days)
    private static final int[] INTERVALS = {0, 1, 3, 7, 14, 30};

    public int getIdTerm() {
        return idTerm;
    }
    public void setIdTerm(int idTerm) {
        this.idTerm = idTerm;
    }
    public String getTerm() {
        return term;
    }
    public void setTerm(String term) {
        this.term = term;
    }
    public String getTranslation() {
        return translation;
    }
    public void setTranslation(String translation) {
        this.translation = translation;
    }
    public String getPronunciation() {
        return pronunciation;
    }
    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }
    public int getState() {
        return state;
    }
    public void setState(int state) {
        this.state = state;
    }
    public String getUrlImg() {
        return urlImg;
    }
    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }
    
    // Getters and setters for spaced repetition fields
    public String getLastReview() {
        return lastReview;
    }
    
    public void setLastReview(String lastReview) {
        this.lastReview = lastReview;
    }
    
    public int getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    // Helper methods for spaced repetition
    
    /**
     * Checks if this word is due for review based on its last review date and current state
     * @return true if the word is due for review, false otherwise
     */
    public boolean isDueForReview() {
        // Si la palabra nunca fue revisada, siempre está lista para practicar
        if (lastReview == null || lastReview.isEmpty()) {
            return true;
        }
        try {
            LocalDate lastReviewDate = LocalDate.parse(lastReview, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate nextReviewDate = getNextReviewDateSafe(lastReviewDate, state);
            return LocalDate.now().isAfter(nextReviewDate) || LocalDate.now().isEqual(nextReviewDate);
        } catch (Exception e) {
            // Si hay error de formato, considera la palabra como debida
            return true;
        }
    }

    // Nueva función robusta para calcular la próxima fecha de revisión
    private LocalDate getNextReviewDateSafe(LocalDate lastReviewDate, int state) {
        int intervalDays = (state >= 0 && state < INTERVALS.length) ? INTERVALS[state] : INTERVALS[0];
        try {
            return lastReviewDate.plusDays(intervalDays);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    /**
     * Calculates the number of days until the next review
     * @return number of days until next review (negative if overdue)
     */
    public int getDaysUntilNextReview() {
        if (lastReview == null || lastReview.isEmpty()) {
            return 0; // Never reviewed words are due now
        }
        
        try {
            LocalDate nextReviewDate = getNextReviewDate();
            return (int) ChronoUnit.DAYS.between(LocalDate.now(), nextReviewDate);
        } catch (Exception e) {
            return 0; // If parsing fails, consider it due now
        }
    }
    
    /**
     * Calculates the success rate for this word
     * @return success rate as a percentage (0.0 to 1.0)
     */
    public double getSuccessRate() {
        if (reviewCount == 0) {
            return 0.0;
        }
        return (double) successCount / reviewCount;
    }
    
    /**
     * Calculates the next review date based on the last review date and current state
     * @return LocalDate representing when this word should be reviewed next
     */
    public LocalDate getNextReviewDate() {
        if (lastReview == null || lastReview.isEmpty()) {
            return LocalDate.now();
        }
        try {
            LocalDate lastReviewDate = LocalDate.parse(lastReview, DateTimeFormatter.ISO_LOCAL_DATE);
            int intervalDays = (state >= 0 && state < INTERVALS.length) ? INTERVALS[state] : INTERVALS[0];
            return lastReviewDate.plusDays(intervalDays);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
} 