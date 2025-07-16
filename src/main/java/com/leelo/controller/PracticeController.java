package com.leelo.controller;

import com.leelo.model.StudySession;
import com.leelo.model.Word;
import com.leelo.service.SpacedRepetitionService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.LinkedList;
import java.util.Queue;

public class PracticeController {
    @FXML private Label wordLabel;
    @FXML private Label translationLabel;
    @FXML private Label pronunciationLabel;
    @FXML private Label progressLabel;
    @FXML private Label sessionStatsLabel;
    @FXML private Label wordStatsLabel;
    @FXML private Label nextReviewLabel;
    @FXML private Label reviewHistoryLabel;
    @FXML private Button showButton;
    @FXML private Button nextButton;
    @FXML private Button backButton;
    @FXML private Button startSessionButton;
    @FXML private Button endSessionButton;
    @FXML private Button correctButton;
    @FXML private Button incorrectButton;
    @FXML private javafx.scene.control.ProgressBar progressBar;
    @FXML private javafx.scene.layout.HBox reviewButtonsBox;

    // Spaced repetition dependencies and variables
    private SpacedRepetitionService spacedRepetitionService;
    private StudySession currentSession;
    private Queue<Word> wordsToReview;
    private Word currentWord;
    private boolean answerShown;

    @FXML
    public void initialize() {
        // Initialize spaced repetition service
        spacedRepetitionService = new SpacedRepetitionService();
        
        // Initialize study session tracking variables
        currentSession = null;
        wordsToReview = new LinkedList<>();
        currentWord = null;
        answerShown = false;
        
        // Initialize UI state - only show Start Session button initially
        updateProgressDisplay();
        updateSessionStatsDisplay();
        hideReviewButtons();
        endSessionButton.setVisible(false);
        showButton.setVisible(false);
        nextButton.setVisible(false);
    }

    // Study session management methods
    
    /**
     * Starts a new study session by loading words due for review
     */
    @FXML
    public void startStudySession() {
        try {
            // Get words due for review from the spaced repetition service
            wordsToReview = new LinkedList<>(spacedRepetitionService.getWordsForReview());

            // Validación extra: filtra palabras con campos críticos nulos o vacíos
            wordsToReview.removeIf(word ->
                word.getTerm() == null || word.getTerm().isEmpty() ||
                word.getTranslation() == null || word.getTranslation().isEmpty() ||
                word.getState() < 0 ||
                word.getReviewCount() < 0 || word.getSuccessCount() < 0
            );

            // Start a new study session
            currentSession = spacedRepetitionService.startStudySession();

            // Update UI for session start
            updateUIForSessionStart();

            if (wordsToReview.isEmpty()) {
                // No words due for review
                wordLabel.setText("No words due for review!");
                translationLabel.setText("Check back later for more words to study.");
                translationLabel.setVisible(true);
                showButton.setDisable(true);
                nextButton.setDisable(true);
            } else {
                // Show the first word
                showNextWord();
                showButton.setDisable(false);
                nextButton.setDisable(false);
            }
        } catch (Exception e) {
            // LOG DETALLADO DEL ERROR
            System.err.println("[PracticeController] Error loading study session:");
            e.printStackTrace();
            wordLabel.setText("Error loading study session");
            translationLabel.setText("Please try again later.\n" + e.getMessage());
            translationLabel.setVisible(true);
            showButton.setDisable(true);
            nextButton.setDisable(true);
        }
    }
    
    /**
     * Displays the next word in the review queue
     */
    public void showNextWord() {
        if (wordsToReview.isEmpty()) {
            // No more words to review - end session
            endStudySession();
            return;
        }
        
        // Get the next word from the queue
        currentWord = wordsToReview.poll();
        answerShown = false;
        
        // Display the English term
        wordLabel.setText(currentWord.getTerm());
        translationLabel.setText("Think of the translation, then click 'Show Answer'");
        translationLabel.setVisible(true);
        pronunciationLabel.setVisible(false);
        
        // Update word statistics display
        updateWordStatsDisplay();
        
        // Update progress and session stats
        updateProgressDisplay();
        updateSessionStatsDisplay();
        
        // Update button states
        showButton.setText("Show Answer");
        showButton.setDisable(false);
        hideReviewButtons();
    }

    /**
     * Handles when user marks a word as correctly recalled
     */
    @FXML
    public void markWordCorrect() {
        if (currentWord == null || !answerShown) {
            return; // Can only mark correct after showing answer
        }
        
        try {
            // Process the correct review result
            spacedRepetitionService.processReviewResult(currentWord, true);
            
            // Update displays
            updateProgressDisplay();
            updateSessionStatsDisplay();
            
            // Move to next word
            showNextWord();
        } catch (Exception e) {
            translationLabel.setText("Error processing review. Please try again.");
        }
    }
    
    /**
     * Handles when user marks a word as incorrectly recalled
     */
    @FXML
    public void markWordIncorrect() {
        if (currentWord == null || !answerShown) {
            return; // Can only mark incorrect after showing answer
        }
        
        try {
            // Process the incorrect review result
            spacedRepetitionService.processReviewResult(currentWord, false);
            
            // Update displays
            updateProgressDisplay();
            updateSessionStatsDisplay();
            
            // Move to next word
            showNextWord();
        } catch (Exception e) {
            translationLabel.setText("Error processing review. Please try again.");
        }
    }
    
    /**
     * Shows the answer for the current word
     */
    @FXML
    public void showAnswer() {
        if (currentWord == null || answerShown) {
            return;
        }
        
        answerShown = true;
        
        // Display the translation
        translationLabel.setText(currentWord.getTranslation());
        translationLabel.setVisible(true);
        
        // Display pronunciation if available
        if (currentWord.getPronunciation() != null && !currentWord.getPronunciation().isEmpty()) {
            pronunciationLabel.setText("[" + currentWord.getPronunciation() + "]");
            pronunciationLabel.setVisible(true);
        }
        
        // Show review feedback buttons
        showReviewButtons();
    }

    /**
     * Ends the current study session and shows results
     */
    @FXML
    public void endStudySession() {
        if (currentSession == null) {
            return;
        }
        
        try {
            // End the study session and get final statistics
            spacedRepetitionService.endStudySession();
            
            // Show session summary
            showSessionSummary();
            
            // Update UI for session end
            updateUIForSessionEnd();
            
            // Reset session state
            currentSession = null;
            wordsToReview.clear();
            currentWord = null;
            answerShown = false;
        } catch (Exception e) {
            translationLabel.setText("Error ending session: " + e.getMessage());
            translationLabel.setVisible(true);
        }
    }
    
    /**
     * Displays session summary with performance statistics
     */
    public void showSessionSummary() {
        if (currentSession == null) {
            return;
        }
        
        try {
            // Get session statistics
            var sessionStats = spacedRepetitionService.getSessionStats();
            
            // Display summary information
            StringBuilder summary = new StringBuilder();
            summary.append("Session Complete!\n\n");
            summary.append("Words Reviewed: ").append(sessionStats.getWordsReviewed()).append("\n");
            summary.append("Correct Answers: ").append(sessionStats.getCorrectAnswers()).append("\n");
            summary.append("Accuracy: ").append(String.format("%.1f%%", sessionStats.getAccuracyPercentage())).append("\n");
            
            if (sessionStats.getSessionDuration() > 0) {
                summary.append("Session Duration: ").append(sessionStats.getSessionDuration()).append(" minutes\n");
            }
            
            wordLabel.setText("Study Session Summary");
            translationLabel.setText(summary.toString());
            
            // Disable action buttons
            showButton.setDisable(true);
            nextButton.setDisable(true);
        } catch (Exception e) {
            wordLabel.setText("Session Complete");
            translationLabel.setText("Unable to load session statistics.");
        }
    }
    
    /**
     * Gets progress information for ongoing session
     */
    public String getSessionProgress() {
        if (currentSession == null) {
            return "No active session";
        }
        
        try {
            var sessionStats = spacedRepetitionService.getSessionStats();
            int remaining = wordsToReview.size();
            int reviewed = sessionStats.getWordsReviewed();
            int total = reviewed + remaining;
            
            return String.format("Progress: %d/%d words (%d remaining)", 
                reviewed, total, remaining);
        } catch (Exception e) {
            return "Progress unavailable";
        }
    }

    // UI Update Methods
    
    /**
     * Updates the progress display with current session information
     */
    private void updateProgressDisplay() {
        if (currentSession == null) {
            progressLabel.setText("Progress: Ready to start");
            progressBar.setProgress(0.0);
            return;
        }
        
        try {
            var sessionStats = spacedRepetitionService.getSessionStats();
            int remaining = wordsToReview.size();
            int reviewed = sessionStats.getWordsReviewed();
            int total = reviewed + remaining;
            
            if (total > 0) {
                double progress = (double) reviewed / total;
                progressBar.setProgress(progress);
                progressLabel.setText(String.format("Progress: %d/%d words (%d remaining)", 
                    reviewed, total, remaining));
            } else {
                progressBar.setProgress(1.0);
                progressLabel.setText("No words to review");
            }
        } catch (Exception e) {
            progressLabel.setText("Progress: Unavailable");
            progressBar.setProgress(0.0);
        }
    }
    
    /**
     * Updates the session statistics display
     */
    private void updateSessionStatsDisplay() {
        if (currentSession == null) {
            sessionStatsLabel.setText("Session Stats: Not started");
            return;
        }
        
        try {
            var sessionStats = spacedRepetitionService.getSessionStats();
            int correct = sessionStats.getCorrectAnswers();
            int total = sessionStats.getWordsReviewed();
            int incorrect = total - correct;
            
            sessionStatsLabel.setText(String.format("Session Stats: %d correct, %d incorrect", 
                correct, incorrect));
        } catch (Exception e) {
            sessionStatsLabel.setText("Session Stats: Unavailable");
        }
    }
    
    /**
     * Updates word-specific statistics and review information
     */
    private void updateWordStatsDisplay() {
        if (currentWord == null) {
            wordStatsLabel.setText("");
            nextReviewLabel.setText("");
            reviewHistoryLabel.setText("");
            return;
        }
        
        try {
            // Display word statistics
            double successRate = currentWord.getSuccessRate();
            int reviewCount = currentWord.getReviewCount();
            wordStatsLabel.setText(String.format("Word Stats: %d reviews, %.1f%% success rate", 
                reviewCount, successRate * 100));
            
            // Display next review information
            if (currentWord.getNextReviewDate() != null) {
                nextReviewLabel.setText("Next review: " + currentWord.getNextReviewDate().toString());
            } else {
                nextReviewLabel.setText("Next review: After this session");
            }
            
            // Display review history
            if (reviewCount > 0) {
                reviewHistoryLabel.setText(String.format("Last reviewed: %s (State: %d)", 
                    currentWord.getLastReview() != null ? currentWord.getLastReview() : "Never", 
                    currentWord.getState()));
            } else {
                reviewHistoryLabel.setText("First time reviewing this word");
            }
        } catch (Exception e) {
            wordStatsLabel.setText("Word stats unavailable");
            nextReviewLabel.setText("");
            reviewHistoryLabel.setText("");
        }
    }
    
    /**
     * Shows the review feedback buttons
     */
    private void showReviewButtons() {
        reviewButtonsBox.setVisible(true);
        showButton.setVisible(false);
    }
    
    /**
     * Hides the review feedback buttons
     */
    private void hideReviewButtons() {
        reviewButtonsBox.setVisible(false);
        showButton.setVisible(true);
    }
    
    /**
     * Updates UI state for session start
     */
    private void updateUIForSessionStart() {
        startSessionButton.setVisible(false);
        endSessionButton.setVisible(true);
        showButton.setVisible(true);
        nextButton.setVisible(true); // Show skip button during session
        hideReviewButtons();
        updateProgressDisplay();
        updateSessionStatsDisplay();
    }
    
    /**
     * Updates UI state for session end
     */
    private void updateUIForSessionEnd() {
        startSessionButton.setVisible(true);
        endSessionButton.setVisible(false);
        hideReviewButtons();
        showButton.setVisible(false);
        nextButton.setVisible(false);
        wordStatsLabel.setText("");
        nextReviewLabel.setText("");
        reviewHistoryLabel.setText("");
        updateProgressDisplay();
        updateSessionStatsDisplay();
    }
    
    // FXML Action Methods
    
    /**
     * Handles skip word action
     */
    @FXML
    public void skipWord() {
        if (currentWord != null && currentSession != null) {
            // Use the SpacedRepetitionService skip functionality
            Word nextWord = spacedRepetitionService.skipCurrentWord();
            if (nextWord != null) {
                showNextWord();
            } else {
                // No more words, end session
                endStudySession();
            }
            updateProgressDisplay();
            updateSessionStatsDisplay();
        }
    }
}