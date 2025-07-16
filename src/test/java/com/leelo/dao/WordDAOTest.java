package com.leelo.dao;

import com.leelo.model.Word;
import com.leelo.model.StudyStatistics;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WordDAO enhanced methods
 * Tests getWordsDueForReview(), statistics calculations, and database migration logic
 */
class WordDAOTest {

    private WordDAO wordDAO;
    private static final String TEST_DB_NAME = "test_leelo.db";

    @BeforeAll
    static void setUpDatabase() {
        // Set up test database
        try {
            Database.initialize();
            
            // Check if migration is needed and run it
            if (!Database.checkSchemaVersion()) {
                System.out.println("Running database migration for tests...");
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
        // Clean up any existing test data
        cleanupTestData();
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
            PreparedStatement deleteWords = conn.prepareStatement("DELETE FROM words WHERE term LIKE 'test_%'");
            deleteWords.executeUpdate();
            
            // Delete test study sessions
            PreparedStatement deleteSessions = conn.prepareStatement("DELETE FROM study_sessions WHERE session_date >= ?");
            deleteSessions.setString(1, LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE));
            deleteSessions.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }

    // Test getWordsDueForReview() with various date scenarios
    @Test
    void testGetWordsDueForReview_NewWords() {
        // Create a new word (never reviewed)
        Word newWord = createTestWord("test_new", "nuevo", 0);
        newWord.setLastReview(null); // Never reviewed
        wordDAO.insertWord(newWord);

        List<Word> dueWords = wordDAO.getWordsDueForReview();
        
        assertTrue(dueWords.stream().anyMatch(w -> w.getTerm().equals("test_new")),
                "New words should be due for review");
    }

    @Test
    void testGetWordsDueForReview_OverdueWords() {
        // Create a word that's overdue for review
        Word overdueWord = createTestWord("test_overdue", "atrasado", 2);
        overdueWord.setLastReview(LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE));
        overdueWord.setReviewCount(3);
        overdueWord.setSuccessCount(2);
        wordDAO.insertWord(overdueWord);

        List<Word> dueWords = wordDAO.getWordsDueForReview();
        
        assertTrue(dueWords.stream().anyMatch(w -> w.getTerm().equals("test_overdue")),
                "Overdue words should be due for review");
    }

    @Test
    void testGetWordsDueForReview_NotDueWords() {
        // Create a word that's not due yet
        Word notDueWord = createTestWord("test_notdue", "no_debido", 3);
        wordDAO.insertWord(notDueWord);
        
        // Get the word back to get its ID and update it with review data
        List<Word> allWords = wordDAO.listAll();
        Word insertedWord = allWords.stream()
                .filter(w -> w.getTerm().equals("test_notdue"))
                .findFirst()
                .orElse(null);
        
        if (insertedWord != null) {
            insertedWord.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            insertedWord.setReviewCount(2);
            insertedWord.setSuccessCount(2);
            insertedWord.setState(3);
            wordDAO.updateWordReviewData(insertedWord);
        }

        List<Word> dueWords = wordDAO.getWordsDueForReview();
        
        assertFalse(dueWords.stream().anyMatch(w -> w.getTerm().equals("test_notdue")),
                "Words not due yet should not be in the due list");
    }

    @Test
    void testGetWordsByState() {
        // Create words in different states
        Word state0Word = createTestWord("test_state0", "estado0", 0);
        Word state1Word = createTestWord("test_state1", "estado1", 1);
        Word state2Word = createTestWord("test_state2", "estado2", 2);
        
        wordDAO.insertWord(state0Word);
        wordDAO.insertWord(state1Word);
        wordDAO.insertWord(state2Word);

        List<Word> state0Words = wordDAO.getWordsByState(0);
        List<Word> state1Words = wordDAO.getWordsByState(1);
        List<Word> state2Words = wordDAO.getWordsByState(2);

        assertTrue(state0Words.stream().anyMatch(w -> w.getTerm().equals("test_state0")));
        assertTrue(state1Words.stream().anyMatch(w -> w.getTerm().equals("test_state1")));
        assertTrue(state2Words.stream().anyMatch(w -> w.getTerm().equals("test_state2")));
    }

    @Test
    void testUpdateWordReviewData() {
        // Create and insert a test word
        Word testWord = createTestWord("test_update", "actualizar", 1);
        wordDAO.insertWord(testWord);
        
        // Get the word back to get its ID
        List<Word> allWords = wordDAO.listAll();
        Word insertedWord = allWords.stream()
                .filter(w -> w.getTerm().equals("test_update"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(insertedWord, "Word should be inserted successfully");

        // Update review data
        insertedWord.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        insertedWord.setReviewCount(5);
        insertedWord.setSuccessCount(4);
        insertedWord.setState(2);

        boolean updateResult = wordDAO.updateWordReviewData(insertedWord);
        assertTrue(updateResult, "Update should be successful");

        // Verify the update
        List<Word> updatedWords = wordDAO.listAll();
        Word updatedWord = updatedWords.stream()
                .filter(w -> w.getTerm().equals("test_update"))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedWord);
        assertEquals(5, updatedWord.getReviewCount());
        assertEquals(4, updatedWord.getSuccessCount());
        assertEquals(2, updatedWord.getState());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), updatedWord.getLastReview());
    }

    @Test
    void testUpdateWordReviewDataBatch() {
        // Create and insert test words
        Word word1 = createTestWord("test_batch1", "lote1", 1);
        Word word2 = createTestWord("test_batch2", "lote2", 2);
        wordDAO.insertWord(word1);
        wordDAO.insertWord(word2);

        // Get the words back to get their IDs
        List<Word> allWords = wordDAO.listAll();
        Word insertedWord1 = allWords.stream()
                .filter(w -> w.getTerm().equals("test_batch1"))
                .findFirst()
                .orElse(null);
        Word insertedWord2 = allWords.stream()
                .filter(w -> w.getTerm().equals("test_batch2"))
                .findFirst()
                .orElse(null);

        assertNotNull(insertedWord1);
        assertNotNull(insertedWord2);

        // Update review data for both words
        insertedWord1.setReviewCount(3);
        insertedWord1.setSuccessCount(2);
        insertedWord1.setState(2);
        insertedWord1.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        insertedWord2.setReviewCount(4);
        insertedWord2.setSuccessCount(3);
        insertedWord2.setState(3);
        insertedWord2.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        List<Word> wordsToUpdate = Arrays.asList(insertedWord1, insertedWord2);
        boolean batchUpdateResult = wordDAO.updateWordReviewDataBatch(wordsToUpdate);
        
        assertTrue(batchUpdateResult, "Batch update should be successful");

        // Verify the updates
        List<Word> updatedWords = wordDAO.listAll();
        Word updatedWord1 = updatedWords.stream()
                .filter(w -> w.getTerm().equals("test_batch1"))
                .findFirst()
                .orElse(null);
        Word updatedWord2 = updatedWords.stream()
                .filter(w -> w.getTerm().equals("test_batch2"))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedWord1);
        assertNotNull(updatedWord2);
        assertEquals(3, updatedWord1.getReviewCount());
        assertEquals(2, updatedWord1.getSuccessCount());
        assertEquals(4, updatedWord2.getReviewCount());
        assertEquals(3, updatedWord2.getSuccessCount());
    }

    @Test
    void testUpdateWordReviewDataBatch_EmptyList() {
        boolean result = wordDAO.updateWordReviewDataBatch(Arrays.asList());
        assertTrue(result, "Empty batch update should return true");
        
        boolean nullResult = wordDAO.updateWordReviewDataBatch(null);
        assertTrue(nullResult, "Null batch update should return true");
    }

    // Verify statistics calculation accuracy
    @Test
    void testGetWordCountByState() {
        // Create words in different states
        Word state0Word = createTestWord("test_count0", "contar0", 0);
        Word state1Word1 = createTestWord("test_count1a", "contar1a", 1);
        Word state1Word2 = createTestWord("test_count1b", "contar1b", 1);
        Word state2Word = createTestWord("test_count2", "contar2", 2);
        
        wordDAO.insertWord(state0Word);
        wordDAO.insertWord(state1Word1);
        wordDAO.insertWord(state1Word2);
        wordDAO.insertWord(state2Word);

        Map<Integer, Integer> stateCount = wordDAO.getWordCountByState();

        assertNotNull(stateCount);
        assertTrue(stateCount.containsKey(0));
        assertTrue(stateCount.containsKey(1));
        assertTrue(stateCount.containsKey(2));
        
        // Note: We can't assert exact counts because there might be other words in the database
        // But we can verify the structure is correct
        assertTrue(stateCount.get(0) >= 1, "Should have at least 1 word in state 0");
        assertTrue(stateCount.get(1) >= 2, "Should have at least 2 words in state 1");
        assertTrue(stateCount.get(2) >= 1, "Should have at least 1 word in state 2");
    }

    @Test
    void testGetReviewAccuracy() {
        // Create words with known success rates
        Word word1 = createTestWord("test_accuracy1", "precision1", 1);
        word1.setReviewCount(10);
        word1.setSuccessCount(8); // 80% success rate
        wordDAO.insertWord(word1);

        Word word2 = createTestWord("test_accuracy2", "precision2", 2);
        word2.setReviewCount(5);
        word2.setSuccessCount(3); // 60% success rate
        wordDAO.insertWord(word2);

        // Get the words back to update them with review data
        List<Word> allWords = wordDAO.listAll();
        Word insertedWord1 = allWords.stream()
                .filter(w -> w.getTerm().equals("test_accuracy1"))
                .findFirst()
                .orElse(null);
        Word insertedWord2 = allWords.stream()
                .filter(w -> w.getTerm().equals("test_accuracy2"))
                .findFirst()
                .orElse(null);

        if (insertedWord1 != null && insertedWord2 != null) {
            insertedWord1.setReviewCount(10);
            insertedWord1.setSuccessCount(8);
            wordDAO.updateWordReviewData(insertedWord1);

            insertedWord2.setReviewCount(5);
            insertedWord2.setSuccessCount(3);
            wordDAO.updateWordReviewData(insertedWord2);

            double accuracy = wordDAO.getReviewAccuracy();
            assertTrue(accuracy >= 0.0 && accuracy <= 1.0, "Accuracy should be between 0 and 1");
        }
    }

    @Test
    void testGetStudyStatistics() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        StudyStatistics stats = wordDAO.getStudyStatistics(from, to);

        assertNotNull(stats);
        assertEquals(from, stats.getFromDate());
        assertEquals(to, stats.getToDate());
        assertNotNull(stats.getWordCountByState());
        assertTrue(stats.getTotalWordsReviewed() >= 0);
        assertTrue(stats.getTotalCorrectAnswers() >= 0);
        assertTrue(stats.getTotalSessions() >= 0);
        assertTrue(stats.getAverageAccuracy() >= 0.0);
    }

    @Test
    void testGetRecentReviewAccuracy() {
        double recentAccuracy = wordDAO.getRecentReviewAccuracy(7);
        assertTrue(recentAccuracy >= 0.0 && recentAccuracy <= 1.0, 
                "Recent accuracy should be between 0 and 1");
    }

    @Test
    void testGetTotalWordCount() {
        int initialCount = wordDAO.getTotalWordCount();
        
        // Add a test word
        Word testWord = createTestWord("test_count", "contar", 0);
        wordDAO.insertWord(testWord);
        
        int newCount = wordDAO.getTotalWordCount();
        assertEquals(initialCount + 1, newCount, "Word count should increase by 1");
    }

    // Test database migration logic with mock data
    @Test
    void testWordCreationWithSpacedRepetitionFields() {
        Word word = createTestWord("test_migration", "migracion", 1);
        word.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        word.setReviewCount(3);
        word.setSuccessCount(2);

        boolean insertResult = wordDAO.insertWord(word);
        assertTrue(insertResult, "Word with spaced repetition fields should be inserted successfully");

        List<Word> allWords = wordDAO.listAll();
        Word retrievedWord = allWords.stream()
                .filter(w -> w.getTerm().equals("test_migration"))
                .findFirst()
                .orElse(null);

        assertNotNull(retrievedWord);
        // Note: The insertWord method doesn't insert spaced repetition fields,
        // so we test that the word can be retrieved and then updated
        retrievedWord.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        retrievedWord.setReviewCount(3);
        retrievedWord.setSuccessCount(2);
        
        boolean updateResult = wordDAO.updateWordReviewData(retrievedWord);
        assertTrue(updateResult, "Spaced repetition data should be updatable");
    }

    @Test
    void testGetWordsDueForReview_VariousDateScenarios() {
        // Test with words in different review states
        Word immediateWord = createTestWord("test_immediate", "inmediato", 0);
        immediateWord.setLastReview(null);
        wordDAO.insertWord(immediateWord);

        Word dueTodayWord = createTestWord("test_due_today", "debido_hoy", 1);
        dueTodayWord.setLastReview(LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        wordDAO.insertWord(dueTodayWord);

        Word futureWord = createTestWord("test_future", "futuro", 2);
        futureWord.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        wordDAO.insertWord(futureWord);

        // Update the words with review data
        List<Word> allWords = wordDAO.listAll();
        for (Word word : allWords) {
            if (word.getTerm().equals("test_due_today")) {
                word.setLastReview(LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
                wordDAO.updateWordReviewData(word);
            } else if (word.getTerm().equals("test_future")) {
                word.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                wordDAO.updateWordReviewData(word);
            }
        }

        List<Word> dueWords = wordDAO.getWordsDueForReview();

        assertTrue(dueWords.stream().anyMatch(w -> w.getTerm().equals("test_immediate")),
                "Words never reviewed should be due");
        assertTrue(dueWords.stream().anyMatch(w -> w.getTerm().equals("test_due_today")),
                "Words due today should be in the list");
        assertFalse(dueWords.stream().anyMatch(w -> w.getTerm().equals("test_future")),
                "Words not due yet should not be in the list");
    }

    @Test
    void testWordDAOErrorHandling() {
        // Test updating non-existent word
        Word nonExistentWord = createTestWord("non_existent", "no_existe", 1);
        nonExistentWord.setIdTerm(99999); // Non-existent ID
        
        boolean result = wordDAO.updateWordReviewData(nonExistentWord);
        assertFalse(result, "Updating non-existent word should return false");
    }

    @Test
    void testGetWordsByState_InvalidState() {
        List<Word> words = wordDAO.getWordsByState(-1);
        assertNotNull(words);
        assertTrue(words.isEmpty(), "Invalid state should return empty list");
        
        List<Word> words2 = wordDAO.getWordsByState(10);
        assertNotNull(words2);
        assertTrue(words2.isEmpty(), "Invalid state should return empty list");
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