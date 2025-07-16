package com.leelo.integration;

import com.leelo.dao.Database;
import com.leelo.dao.WordDAO;
import com.leelo.dao.StudySessionDAO;
import com.leelo.model.Word;
import com.leelo.model.StudySession;
import com.leelo.service.SpacedRepetitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete study workflow
 * Tests full study session from start to finish, data persistence, and error handling
 */
class StudyWorkflowIntegrationTest {

    private WordDAO wordDAO;
    private StudySessionDAO studySessionDAO;
    private SpacedRepetitionService spacedRepetitionService;

    @BeforeAll
    static void setUpDatabase() {
        // Set up test database
        try {
            Database.initialize();
            
            // Check if migration is needed and run it
            if (!Database.checkSchemaVersion()) {
                System.out.println("Running database migration for integration tests...");
                boolean migrationSuccess = Database.migrateToSpacedRepetition();
                if (!migrationSuccess) {
                    throw new RuntimeException("Database migration failed");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize test database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        wordDAO = new WordDAO();
        studySessionDAO = new StudySessionDAO();
        spacedRepetitionService = new SpacedRepetitionService(wordDAO);
        
        // Clean up any existing test data
        cleanupTestData();
        
        // Set up test data
        setupTestWords();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data after each test
        cleanupTestData();
    }

    @AfterAll
    static void tearDownDatabase() {
        // Clean up test database
        cleanupTestData();
    }

    private static void cleanupTestData() {
        try (Connection conn = Database.getConnection()) {
            // Delete test words
            PreparedStatement deleteWords = conn.prepareStatement("DELETE FROM words WHERE term LIKE 'integration_%'");
            deleteWords.executeUpdate();
            
            // Delete test study sessions
            PreparedStatement deleteSessions = conn.prepareStatement("DELETE FROM study_sessions WHERE session_date >= ?");
            deleteSessions.setString(1, LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
            deleteSessions.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }

    private void setupTestWords() {
        // Create test words in different states for comprehensive testing
        Word newWord = createTestWord("integration_new", "nuevo", 0);
        Word learningWord = createTestWord("integration_learning", "aprendiendo", 1);
        Word familiarWord = createTestWord("integration_familiar", "familiar", 2);
        Word knownWord = createTestWord("integration_known", "conocido", 3);
        
        // Insert words
        wordDAO.insertWord(newWord);
        wordDAO.insertWord(learningWord);
        wordDAO.insertWord(familiarWord);
        wordDAO.insertWord(knownWord);
        
        // Set up review data for some words to make them due
        List<Word> allWords = wordDAO.listAll();
        for (Word word : allWords) {
            if (word.getTerm().equals("integration_learning")) {
                word.setLastReview(LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
                word.setReviewCount(1);
                word.setSuccessCount(1);
                word.setState(1);
                wordDAO.updateWordReviewData(word);
            } else if (word.getTerm().equals("integration_familiar")) {
                word.setLastReview(LocalDate.now().minusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE));
                word.setReviewCount(2);
                word.setSuccessCount(2);
                word.setState(2);
                wordDAO.updateWordReviewData(word);
            }
        }
    }

    // Test full study session from start to finish
    @Test
    void testCompleteStudySessionWorkflow() {
        // Start a study session
        StudySession session = spacedRepetitionService.startStudySession(10);
        assertNotNull(session, "Study session should start successfully");
        assertEquals(0, session.getWordsReviewed(), "Initial words reviewed should be 0");
        assertEquals(0, session.getCorrectAnswers(), "Initial correct answers should be 0");

        // Process words in the session
        int wordsProcessed = 0;
        int correctAnswers = 0;
        
        while (!spacedRepetitionService.isSessionComplete() && wordsProcessed < 5) {
            Word currentWord = spacedRepetitionService.getNextSessionWord();
            assertNotNull(currentWord, "Should have a word to review");
            
            // Simulate answering correctly for some words, incorrectly for others
            boolean isCorrect = wordsProcessed % 2 == 0; // Alternate correct/incorrect
            boolean processResult = spacedRepetitionService.processSessionReview(currentWord, isCorrect);
            assertTrue(processResult, "Review processing should succeed");
            
            if (isCorrect) {
                correctAnswers++;
            }
            wordsProcessed++;
            
            // Verify session stats are updated
            StudySession currentStats = spacedRepetitionService.getSessionStats();
            assertNotNull(currentStats);
            assertEquals(wordsProcessed, currentStats.getWordsReviewed());
            assertEquals(correctAnswers, currentStats.getCorrectAnswers());
        }

        // End the study session
        StudySession completedSession = spacedRepetitionService.endStudySession();
        assertNotNull(completedSession, "Session should end successfully");
        assertEquals(wordsProcessed, completedSession.getWordsReviewed());
        assertEquals(correctAnswers, completedSession.getCorrectAnswers());
        assertTrue(completedSession.getSessionDuration() >= 0, "Session duration should be non-negative");

        // Verify session is marked as complete
        assertTrue(spacedRepetitionService.isSessionComplete(), "Session should be marked as complete");
        assertNull(spacedRepetitionService.getSessionStats(), "Session stats should be null after completion");
    }

    @Test
    void testStudySessionWithNoWordsAvailable() {
        // Clean up all test words to simulate no words due
        cleanupTestData();
        
        // Try to start a session with no words available
        StudySession session = spacedRepetitionService.startStudySession(10);
        assertNull(session, "Should return null when no words are due for review");
        
        // Verify session state
        assertTrue(spacedRepetitionService.isSessionComplete(), "Session should be considered complete");
        assertNull(spacedRepetitionService.getNextSessionWord(), "Should have no words to review");
        assertEquals(0, spacedRepetitionService.getRemainingWordsCount(), "Should have 0 remaining words");
    }

    @Test
    void testWordStateTransitionsInSession() {
        // Start a session
        StudySession session = spacedRepetitionService.startStudySession(5);
        assertNotNull(session);

        // Get a word and verify its initial state
        Word testWord = spacedRepetitionService.getNextSessionWord();
        assertNotNull(testWord);
        int initialState = testWord.getState();

        // Process correct answer and verify state advancement
        spacedRepetitionService.processSessionReview(testWord, true);
        
        // Retrieve the word from database to verify state change
        List<Word> updatedWords = wordDAO.listAll();
        Word updatedWord = updatedWords.stream()
                .filter(w -> w.getIdTerm() == testWord.getIdTerm())
                .findFirst()
                .orElse(null);
        
        assertNotNull(updatedWord);
        assertEquals(Math.min(initialState + 1, 5), updatedWord.getState(), 
                "Word state should advance after correct answer");
        
        assertTrue(updatedWord.getReviewCount() >= testWord.getReviewCount(),
                "Review count should not decrease");
        assertTrue(updatedWord.getSuccessCount() >= testWord.getSuccessCount(),
                "Success count should not decrease");
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), 
                updatedWord.getLastReview(), "Last review date should be updated");
    }

    @Test
    void testIncorrectAnswerResetsState() {
        // Start a session
        StudySession session = spacedRepetitionService.startStudySession(5);
        assertNotNull(session);

        // Get a word that's not in state 0
        Word testWord = null;
        while (!spacedRepetitionService.isSessionComplete()) {
            Word currentWord = spacedRepetitionService.getNextSessionWord();
            if (currentWord != null && currentWord.getState() > 0) {
                testWord = currentWord;
                break;
            }
            // Skip this word if it's in state 0
            spacedRepetitionService.skipCurrentWord();
        }

        if (testWord != null) {
            int initialState = testWord.getState();
            assertTrue(initialState > 0, "Test word should have state > 0");

            // Process incorrect answer
            spacedRepetitionService.processSessionReview(testWord, false);
            
            // Retrieve the word from database to verify state reset
            final int testWordId = testWord.getIdTerm(); // Make effectively final for lambda
            List<Word> updatedWords = wordDAO.listAll();
            Word updatedWord = updatedWords.stream()
                    .filter(w -> w.getIdTerm() == testWordId)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(updatedWord);
            assertEquals(0, updatedWord.getState(), "Word state should reset to 0 after incorrect answer");
            assertTrue(updatedWord.getReviewCount() >= testWord.getReviewCount(),
                    "Review count should not decrease");
            assertEquals(testWord.getSuccessCount(), updatedWord.getSuccessCount(),
                    "Success count should not change for incorrect answer");
        }
    }

    // Verify data persistence across session boundaries
    @Test
    void testDataPersistenceAcrossSessionBoundaries() {
        // First session: review some words
        StudySession firstSession = spacedRepetitionService.startStudySession(3);
        assertNotNull(firstSession);

        int firstSessionWords = 0;
        int firstSessionCorrect = 0;
        
        while (!spacedRepetitionService.isSessionComplete() && firstSessionWords < 2) {
            Word currentWord = spacedRepetitionService.getNextSessionWord();
            if (currentWord != null) {
                boolean isCorrect = firstSessionWords == 0; // First word correct, second incorrect
                spacedRepetitionService.processSessionReview(currentWord, isCorrect);
                if (isCorrect) firstSessionCorrect++;
                firstSessionWords++;
            }
        }

        StudySession completedFirstSession = spacedRepetitionService.endStudySession();
        assertNotNull(completedFirstSession);

        // Verify first session data
        assertEquals(firstSessionWords, completedFirstSession.getWordsReviewed());
        assertEquals(firstSessionCorrect, completedFirstSession.getCorrectAnswers());

        // Start second session and verify data persistence
        StudySession secondSession = spacedRepetitionService.startStudySession(5);
        
        // The words reviewed in the first session should have updated states and review dates
        List<Word> allWords = wordDAO.listAll();
        long wordsWithTodayReview = allWords.stream()
                .filter(w -> w.getTerm().startsWith("integration_"))
                .filter(w -> LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE).equals(w.getLastReview()))
                .count();
        
        assertEquals(firstSessionWords, wordsWithTodayReview, 
                "Number of words with today's review date should match first session words reviewed");
    }

    @Test
    void testSessionProgressTracking() {
        // Start a session with known number of words
        StudySession session = spacedRepetitionService.startStudySession(10);
        assertNotNull(session);

        // Get initial progress
        SpacedRepetitionService.SessionProgress initialProgress = spacedRepetitionService.getSessionProgress();
        assertNotNull(initialProgress);
        assertEquals(0, initialProgress.getCompletedWords());
        assertTrue(initialProgress.getTotalWords() > 0);
        assertEquals(0.0, initialProgress.getProgressPercentage());

        // Process one word
        Word firstWord = spacedRepetitionService.getNextSessionWord();
        if (firstWord != null) {
            spacedRepetitionService.processSessionReview(firstWord, true);
            
            // Check progress after one word
            SpacedRepetitionService.SessionProgress updatedProgress = spacedRepetitionService.getSessionProgress();
            assertNotNull(updatedProgress);
            assertEquals(1, updatedProgress.getCompletedWords());
            assertTrue(updatedProgress.getProgressPercentage() > 0);
            assertEquals(updatedProgress.getTotalWords() - 1, updatedProgress.getRemainingWords());
        }
    }

    @Test
    void testSessionSkipFunctionality() {
        // Start a session
        StudySession session = spacedRepetitionService.startStudySession(5);
        assertNotNull(session);

        // Get initial word count
        int initialRemainingWords = spacedRepetitionService.getRemainingWordsCount();
        
        // Skip a word
        Word skippedWord = spacedRepetitionService.skipCurrentWord();
        
        if (skippedWord != null) {
            // Verify remaining words count decreased
            assertEquals(initialRemainingWords - 1, spacedRepetitionService.getRemainingWordsCount());
            
            // Verify session stats didn't change (word wasn't processed)
            StudySession stats = spacedRepetitionService.getSessionStats();
            assertEquals(0, stats.getWordsReviewed(), "Skipped words should not count as reviewed");
        }
    }

    // Test error handling and recovery scenarios
    @Test
    void testErrorHandlingWithInvalidWordData() {
        // Start a session
        StudySession session = spacedRepetitionService.startStudySession(5);
        assertNotNull(session);

        // Get a word and modify it to have invalid ID
        Word testWord = spacedRepetitionService.getNextSessionWord();
        if (testWord != null) {
            int originalId = testWord.getIdTerm();
            testWord.setIdTerm(-1); // Invalid ID
            
            // Try to process review with invalid word
            boolean result = spacedRepetitionService.processSessionReview(testWord, true);
            assertFalse(result, "Processing review with invalid word should fail");
            
            // Verify session stats weren't corrupted
            StudySession stats = spacedRepetitionService.getSessionStats();
            assertEquals(0, stats.getWordsReviewed(), "Failed review should not increment stats");
            
            // Restore valid ID and try again
            testWord.setIdTerm(originalId);
            boolean validResult = spacedRepetitionService.processSessionReview(testWord, true);
            assertTrue(validResult, "Processing review with valid word should succeed");
        }
    }

    @Test
    void testSessionRecoveryAfterInterruption() {
        // Start a session and process some words
        StudySession session = spacedRepetitionService.startStudySession(5);
        assertNotNull(session);

        int wordsProcessed = 0;
        while (!spacedRepetitionService.isSessionComplete() && wordsProcessed < 2) {
            Word currentWord = spacedRepetitionService.getNextSessionWord();
            if (currentWord != null) {
                spacedRepetitionService.processSessionReview(currentWord, true);
                wordsProcessed++;
            }
        }

        // Simulate interruption by creating new service instance
        SpacedRepetitionService newService = new SpacedRepetitionService(wordDAO);
        
        // Verify new service starts fresh (no active session)
        assertNull(newService.getSessionStats(), "New service should have no active session");
        assertTrue(newService.isSessionComplete(), "New service should consider session complete");
        
        // Should be able to start new session
        StudySession newSession = newService.startStudySession(5);
        assertNotNull(newSession, "Should be able to start new session after interruption");
    }

    @Test
    void testConcurrentSessionHandling() {
        // Start first session
        StudySession firstSession = spacedRepetitionService.startStudySession(5);
        assertNotNull(firstSession);

        // Try to start another session (should replace the first one)
        StudySession secondSession = spacedRepetitionService.startStudySession(3);
        assertNotNull(secondSession);

        // Verify only one session is active
        StudySession activeSession = spacedRepetitionService.getSessionStats();
        assertNotNull(activeSession);
        
        // The active session should be the second one (newer)
        // We can't directly compare objects, but we can verify behavior
        assertFalse(spacedRepetitionService.isSessionComplete(), "Should have active session");
    }

    @Test
    void testEmptySessionHandling() {
        // Clean up all words to create empty session scenario
        cleanupTestData();
        
        // Try various operations with no active session
        assertNull(spacedRepetitionService.getSessionStats(), "Should have no session stats");
        assertTrue(spacedRepetitionService.isSessionComplete(), "Should be considered complete");
        assertNull(spacedRepetitionService.getNextSessionWord(), "Should have no next word");
        assertEquals(0, spacedRepetitionService.getRemainingWordsCount(), "Should have 0 remaining words");
        
        // Try to process review without active session
        Word dummyWord = createTestWord("dummy", "ficticio", 0);
        boolean result = spacedRepetitionService.processSessionReview(dummyWord, true);
        assertFalse(result, "Should not be able to process review without active session");
        
        // Try to end non-existent session
        StudySession endResult = spacedRepetitionService.endStudySession();
        assertNull(endResult, "Should return null when ending non-existent session");
    }

    @Test
    void testLargeSessionHandling() {
        // Create many test words
        for (int i = 0; i < 20; i++) {
            Word word = createTestWord("integration_large_" + i, "grande_" + i, 0);
            wordDAO.insertWord(word);
        }

        // Start session with all words
        StudySession session = spacedRepetitionService.startStudySession(25);
        assertNotNull(session);

        // Process all words
        int processed = 0;
        while (!spacedRepetitionService.isSessionComplete() && processed < 25) {
            Word currentWord = spacedRepetitionService.getNextSessionWord();
            if (currentWord != null) {
                spacedRepetitionService.processSessionReview(currentWord, processed % 3 == 0); // Vary answers
                processed++;
            } else {
                break;
            }
        }

        // End session and verify
        StudySession completedSession = spacedRepetitionService.endStudySession();
        assertNotNull(completedSession);
        assertTrue(completedSession.getWordsReviewed() > 0, "Should have processed some words");
        assertTrue(completedSession.getSessionDuration() >= 0, "Should have valid duration");
    }

    // Helper method to create test words
    private Word createTestWord(String term, String translation, int state) {
        Word word = new Word();
        word.setTerm(term);
        word.setTranslation(translation);
        word.setPronunciation("[" + term + "]");
        word.setState(state);
        word.setUrlImg("test_image.jpg");
        word.setReviewCount(0);
        word.setSuccessCount(0);
        return word;
    }
}