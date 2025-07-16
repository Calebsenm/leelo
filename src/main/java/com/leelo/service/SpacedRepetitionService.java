package com.leelo.service;

import com.leelo.dao.WordDAO;
import com.leelo.model.Word;
import com.leelo.model.StudySession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class implementing the core spaced repetition algorithm
 * Manages word state transitions and review scheduling based on performance
 */
public class SpacedRepetitionService {
    
    private final WordDAO wordDAO;
    
    // Study session tracking
    private StudySession currentSession;
    private LocalDateTime sessionStartTime;
    private List<Word> sessionWords;
    private int currentWordIndex;
    
    // Spaced repetition intervals for each state (in days)
    // State 0: New words (immediate review)
    // State 1: 1 day interval
    // State 2: 3 days interval  
    // State 3: 7 days interval
    // State 4: 14 days interval
    // State 5: 30 days interval (mastered)
    private static final int[] INTERVALS = {0, 1, 3, 7, 14, 30};
    
    // Maximum state level (mastered)
    private static final int MAX_STATE = 5;
    
    /**
     * Constructor with WordDAO dependency
     * @param wordDAO WordDAO instance for database operations
     */
    public SpacedRepetitionService(WordDAO wordDAO) {
        this.wordDAO = wordDAO;
    }
    
    /**
     * Default constructor that creates its own WordDAO instance
     */
    public SpacedRepetitionService() {
        this.wordDAO = new WordDAO();
    }
    
    /**
     * Calculates the next learning state for a word based on review result
     * @param currentState Current learning state of the word (0-5)
     * @param correct Whether the word was answered correctly
     * @return New learning state after the review
     */
    public int calculateNextState(int currentState, boolean correct) {
        if (correct) {
            // Correct answer: advance to next state (up to maximum)
            return Math.min(currentState + 1, MAX_STATE);
        } else {
            // Incorrect answer: reset to state 0 (needs immediate review)
            return 0;
        }
    }
    
    /**
     * Calculates the next review date based on the learning state
     * @param state Learning state (0-5)
     * @return LocalDate representing when the word should be reviewed next
     */
    public LocalDate calculateNextReviewDate(int state) {
        if (state < 0 || state >= INTERVALS.length) {
            // Invalid state, default to immediate review
            return LocalDate.now();
        }
        
        int intervalDays = INTERVALS[state];
        return LocalDate.now().plusDays(intervalDays);
    }
    
    /**
     * Gets the review interval in days for a specific state
     * @param state Learning state (0-5)
     * @return Number of days until next review for this state
     */
    public int getIntervalForState(int state) {
        if (state < 0 || state >= INTERVALS.length) {
            return INTERVALS[0]; // Default to immediate review
        }
        return INTERVALS[state];
    }
    
    /**
     * Checks if a word is considered mastered (reached maximum state)
     * @param state Current learning state
     * @return true if the word is mastered, false otherwise
     */
    public boolean isWordMastered(int state) {
        return state >= MAX_STATE;
    }
    
    /**
     * Gets the maximum learning state
     * @return Maximum state value
     */
    public int getMaxState() {
        return MAX_STATE;
    }
    
    /**
     * Calculates the difficulty level of a word based on its success rate
     * @param successCount Number of correct reviews
     * @param totalReviews Total number of reviews
     * @return Difficulty level: "Easy", "Medium", "Hard", or "New"
     */
    public String calculateDifficultyLevel(int successCount, int totalReviews) {
        if (totalReviews == 0) {
            return "New";
        }
        
        double successRate = (double) successCount / totalReviews;
        
        if (successRate >= 0.8) {
            return "Easy";
        } else if (successRate >= 0.6) {
            return "Medium";
        } else {
            return "Hard";
        }
    }
    
    /**
     * Determines the priority level for word review based on state and overdue status
     * Lower numbers indicate higher priority
     * @param word Word to evaluate
     * @return Priority level (0 = highest priority, 5 = lowest priority)
     */
    public int calculateReviewPriority(Word word) {
        int state = word.getState();
        boolean isDue = word.isDueForReview();
        int daysUntilNext = word.getDaysUntilNextReview();
        
        // Overdue words get highest priority, with lower states having higher priority
        if (isDue && daysUntilNext < 0) {
            // Overdue: priority based on how overdue and state (lower state = higher priority)
            return Math.max(0, state - Math.abs(daysUntilNext));
        } else if (isDue) {
            // Due today: priority based on state
            return state;
        } else {
            // Not due yet: lowest priority
            return MAX_STATE + 1;
        }
    }
    
    /**
     * Updates a word's review data after a review session
     * @param word Word that was reviewed
     * @param correct Whether the answer was correct
     * @return Updated word with new state and review data
     */
    public Word updateWordAfterReview(Word word, boolean correct) {
        // Update review statistics
        word.setReviewCount(word.getReviewCount() + 1);
        if (correct) {
            word.setSuccessCount(word.getSuccessCount() + 1);
        }
        
        // Calculate new state
        int newState = calculateNextState(word.getState(), correct);
        word.setState(newState);
        
        // Set last review date to today
        word.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        return word;
    }
    
    /**
     * Gets a description of what each learning state represents
     * @param state Learning state (0-5)
     * @return Human-readable description of the state
     */
    public String getStateDescription(int state) {
        switch (state) {
            case 0:
                return "New/Needs Review";
            case 1:
                return "Learning (1 day)";
            case 2:
                return "Familiar (3 days)";
            case 3:
                return "Known (1 week)";
            case 4:
                return "Well Known (2 weeks)";
            case 5:
                return "Mastered (1 month)";
            default:
                return "Unknown State";
        }
    }
    
    /**
     * Retrieves words that are due for review, sorted by priority
     * @return List of words due for review, prioritized by learning state and overdue status
     */
    public List<Word> getWordsForReview() {
        List<Word> dueWords = wordDAO.getWordsDueForReview();
        
        // Sort by priority: lower states and overdue words get higher priority
        return dueWords.stream()
                .sorted(Comparator.comparingInt(this::calculateReviewPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * Retrieves a limited number of words for review session
     * @param maxWords Maximum number of words to return
     * @return List of prioritized words for review (up to maxWords)
     */
    public List<Word> getWordsForReview(int maxWords) {
        List<Word> allDueWords = getWordsForReview();
        
        if (allDueWords.size() <= maxWords) {
            return allDueWords;
        }
        
        return allDueWords.subList(0, maxWords);
    }
    
    /**
     * Retrieves words for review filtered by specific learning states
     * @param states Array of learning states to include (0-5)
     * @param maxWords Maximum number of words to return
     * @return List of words from specified states that are due for review
     */
    public List<Word> getWordsForReviewByStates(int[] states, int maxWords) {
        List<Word> filteredWords = new ArrayList<>();
        
        for (int state : states) {
            List<Word> stateWords = wordDAO.getWordsByState(state);
            // Filter to only include words that are due for review
            List<Word> dueStateWords = stateWords.stream()
                    .filter(Word::isDueForReview)
                    .collect(Collectors.toList());
            filteredWords.addAll(dueStateWords);
        }
        
        // Sort by priority and limit results
        return filteredWords.stream()
                .sorted(Comparator.comparingInt(this::calculateReviewPriority))
                .limit(maxWords)
                .collect(Collectors.toList());
    }
    
    /**
     * Processes the result of a word review and updates the word in the database
     * @param word Word that was reviewed
     * @param correct Whether the answer was correct
     * @return true if the word was successfully updated, false otherwise
     */
    public boolean processReviewResult(Word word, boolean correct) {
        // Update word with new review data
        Word updatedWord = updateWordAfterReview(word, correct);
        
        // Save updated word to database
        return wordDAO.updateWordReviewData(updatedWord);
    }
    
    /**
     * Processes multiple review results in a batch operation
     * @param reviewResults List of WordReviewResult objects containing word and result
     * @return true if all words were successfully updated, false otherwise
     */
    public boolean processReviewResultsBatch(List<WordReviewResult> reviewResults) {
        List<Word> updatedWords = new ArrayList<>();
        
        for (WordReviewResult result : reviewResults) {
            Word updatedWord = updateWordAfterReview(result.getWord(), result.isCorrect());
            updatedWords.add(updatedWord);
        }
        
        return wordDAO.updateWordReviewDataBatch(updatedWords);
    }
    
    /**
     * Gets words that need immediate attention (overdue or in state 0)
     * @return List of words that should be prioritized for review
     */
    public List<Word> getHighPriorityWords() {
        List<Word> allWords = wordDAO.listAll();
        
        return allWords.stream()
                .filter(word -> word.getState() == 0 || 
                               (word.isDueForReview() && word.getDaysUntilNextReview() < 0))
                .sorted(Comparator.comparingInt(this::calculateReviewPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of words due for review by learning state
     * @return Map with state as key and count of due words as value
     */
    public java.util.Map<Integer, Integer> getDueWordCountByState() {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        
        for (int state = 0; state <= MAX_STATE; state++) {
            List<Word> stateWords = wordDAO.getWordsByState(state);
            long dueCount = stateWords.stream()
                    .filter(Word::isDueForReview)
                    .count();
            counts.put(state, (int) dueCount);
        }
        
        return counts;
    }
    
    /**
     * Starts a new study session with a specified number of words
     * @param maxWords Maximum number of words to include in the session
     * @return StudySession object representing the new session, or null if no words are due
     */
    public StudySession startStudySession(int maxWords) {
        // Get words for review
        sessionWords = getWordsForReview(maxWords);
        
        if (sessionWords.isEmpty()) {
            return null; // No words due for review
        }
        
        // Initialize session tracking
        sessionStartTime = LocalDateTime.now();
        currentWordIndex = 0;
        
        // Create new study session
        currentSession = new StudySession();
        currentSession.setSessionDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        currentSession.setWordsReviewed(0);
        currentSession.setCorrectAnswers(0);
        currentSession.setSessionDuration(0);
        currentSession.setReviewedWords(new ArrayList<>());
        
        return currentSession;
    }
    
    /**
     * Starts a study session with default maximum of 20 words
     * @return StudySession object representing the new session, or null if no words are due
     */
    public StudySession startStudySession() {
        return startStudySession(20);
    }
    
    /**
     * Ends the current study session and calculates final statistics
     * @return Completed StudySession with final statistics, or null if no session is active
     */
    public StudySession endStudySession() {
        if (currentSession == null || sessionStartTime == null) {
            return null; // No active session
        }
        
        // Calculate session duration in minutes
        LocalDateTime endTime = LocalDateTime.now();
        long durationMinutes = java.time.Duration.between(sessionStartTime, endTime).toMinutes();
        currentSession.setSessionDuration((int) durationMinutes);
        
        // Finalize session data
        StudySession completedSession = currentSession;
        
        // Reset session tracking
        currentSession = null;
        sessionStartTime = null;
        sessionWords = null;
        currentWordIndex = 0;
        
        return completedSession;
    }
    
    /**
     * Gets the current study session statistics
     * @return Current StudySession with up-to-date statistics, or null if no session is active
     */
    public StudySession getSessionStats() {
        if (currentSession == null) {
            return null;
        }
        
        // Update current session duration
        if (sessionStartTime != null) {
            LocalDateTime currentTime = LocalDateTime.now();
            long durationMinutes = java.time.Duration.between(sessionStartTime, currentTime).toMinutes();
            currentSession.setSessionDuration((int) durationMinutes);
        }
        
        return currentSession;
    }
    
    /**
     * Gets the next word in the current study session
     * @return Next Word to review, or null if session is complete or no session is active
     */
    public Word getNextSessionWord() {
        if (currentSession == null || sessionWords == null || currentWordIndex >= sessionWords.size()) {
            return null;
        }
        
        return sessionWords.get(currentWordIndex);
    }
    
    /**
     * Processes a review result within the current study session
     * @param word Word that was reviewed
     * @param correct Whether the answer was correct
     * @return true if the review was processed successfully, false otherwise
     */
    public boolean processSessionReview(Word word, boolean correct) {
        if (currentSession == null) {
            return false;
        }
        
        // Process the review result
        boolean success = processReviewResult(word, correct);
        
        if (success) {
            // Update session statistics
            currentSession.setWordsReviewed(currentSession.getWordsReviewed() + 1);
            if (correct) {
                currentSession.setCorrectAnswers(currentSession.getCorrectAnswers() + 1);
            }
            
            // Add word to reviewed words list
            currentSession.addReviewedWord(word);
            
            // Move to next word
            currentWordIndex++;
        }
        
        return success;
    }
    
    /**
     * Checks if the current study session is complete
     * @return true if all words in the session have been reviewed, false otherwise
     */
    public boolean isSessionComplete() {
        if (currentSession == null || sessionWords == null) {
            return true;
        }
        
        return currentWordIndex >= sessionWords.size();
    }
    
    /**
     * Gets the progress of the current study session
     * @return SessionProgress object with current progress information
     */
    public SessionProgress getSessionProgress() {
        if (currentSession == null || sessionWords == null) {
            return new SessionProgress(0, 0, 0.0);
        }
        
        int totalWords = sessionWords.size();
        int completedWords = currentWordIndex;
        double progressPercentage = totalWords > 0 ? (double) completedWords / totalWords * 100.0 : 0.0;
        
        return new SessionProgress(completedWords, totalWords, progressPercentage);
    }
    
    /**
     * Skips the current word in the study session without marking it as reviewed
     * @return Next Word to review, or null if session is complete
     */
    public Word skipCurrentWord() {
        if (currentSession == null || sessionWords == null) {
            return null;
        }
        
        currentWordIndex++;
        return getNextSessionWord();
    }
    
    /**
     * Gets the remaining words in the current study session
     * @return Number of words left to review in the current session
     */
    public int getRemainingWordsCount() {
        if (currentSession == null || sessionWords == null) {
            return 0;
        }
        
        return Math.max(0, sessionWords.size() - currentWordIndex);
    }
    
    /**
     * Inner class to hold word review results for batch processing
     */
    public static class WordReviewResult {
        private final Word word;
        private final boolean correct;
        
        public WordReviewResult(Word word, boolean correct) {
            this.word = word;
            this.correct = correct;
        }
        
        public Word getWord() {
            return word;
        }
        
        public boolean isCorrect() {
            return correct;
        }
    }
    
    /**
     * Inner class to hold session progress information
     */
    public static class SessionProgress {
        private final int completedWords;
        private final int totalWords;
        private final double progressPercentage;
        
        public SessionProgress(int completedWords, int totalWords, double progressPercentage) {
            this.completedWords = completedWords;
            this.totalWords = totalWords;
            this.progressPercentage = progressPercentage;
        }
        
        public int getCompletedWords() {
            return completedWords;
        }
        
        public int getTotalWords() {
            return totalWords;
        }
        
        public double getProgressPercentage() {
            return progressPercentage;
        }
        
        public int getRemainingWords() {
            return totalWords - completedWords;
        }
        
        @Override
        public String toString() {
            return String.format("Progress: %d/%d words (%.1f%%)", 
                    completedWords, totalWords, progressPercentage);
        }
    }
}