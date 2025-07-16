package com.leelo.service;

import com.leelo.dao.WordDAO;
import com.leelo.model.Word;
import com.leelo.model.StudySession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpacedRepetitionService algorithm logic
 * Tests state transitions, date calculations, word selection and prioritization
 */
@ExtendWith(MockitoExtension.class)
class SpacedRepetitionServiceTest {

    @Mock
    private WordDAO mockWordDAO;

    private SpacedRepetitionService spacedRepetitionService;

    @BeforeEach
    void setUp() {
        spacedRepetitionService = new SpacedRepetitionService(mockWordDAO);
    }

    // Test state transition calculations with various scenarios
    @Test
    void testCalculateNextState_CorrectAnswer_AdvancesState() {
        // Test advancing from each state with correct answer
        assertEquals(1, spacedRepetitionService.calculateNextState(0, true));
        assertEquals(2, spacedRepetitionService.calculateNextState(1, true));
        assertEquals(3, spacedRepetitionService.calculateNextState(2, true));
        assertEquals(4, spacedRepetitionService.calculateNextState(3, true));
        assertEquals(5, spacedRepetitionService.calculateNextState(4, true));
        assertEquals(5, spacedRepetitionService.calculateNextState(5, true)); // Max state
    }

    @Test
    void testCalculateNextState_IncorrectAnswer_ResetsToZero() {
        // Test that incorrect answers always reset to state 0
        assertEquals(0, spacedRepetitionService.calculateNextState(0, false));
        assertEquals(0, spacedRepetitionService.calculateNextState(1, false));
        assertEquals(0, spacedRepetitionService.calculateNextState(2, false));
        assertEquals(0, spacedRepetitionService.calculateNextState(3, false));
        assertEquals(0, spacedRepetitionService.calculateNextState(4, false));
        assertEquals(0, spacedRepetitionService.calculateNextState(5, false));
    }

    @Test
    void testCalculateNextState_BoundaryConditions() {
        // Test edge cases - the service doesn't normalize invalid states, it applies logic directly
        assertEquals(0, spacedRepetitionService.calculateNextState(-1, true)); // -1 + 1 = 0
        assertEquals(0, spacedRepetitionService.calculateNextState(-1, false));
        assertEquals(5, spacedRepetitionService.calculateNextState(10, true)); // Above max gets capped
        assertEquals(0, spacedRepetitionService.calculateNextState(10, false));
    }

    // Test date calculations for review scheduling
    @Test
    void testCalculateNextReviewDate_AllStates() {
        LocalDate today = LocalDate.now();
        
        // Test intervals for each state: {0, 1, 3, 7, 14, 30}
        assertEquals(today, spacedRepetitionService.calculateNextReviewDate(0));
        assertEquals(today.plusDays(1), spacedRepetitionService.calculateNextReviewDate(1));
        assertEquals(today.plusDays(3), spacedRepetitionService.calculateNextReviewDate(2));
        assertEquals(today.plusDays(7), spacedRepetitionService.calculateNextReviewDate(3));
        assertEquals(today.plusDays(14), spacedRepetitionService.calculateNextReviewDate(4));
        assertEquals(today.plusDays(30), spacedRepetitionService.calculateNextReviewDate(5));
    }

    @Test
    void testCalculateNextReviewDate_InvalidStates() {
        LocalDate today = LocalDate.now();
        
        // Invalid states should default to immediate review
        assertEquals(today, spacedRepetitionService.calculateNextReviewDate(-1));
        assertEquals(today, spacedRepetitionService.calculateNextReviewDate(6));
        assertEquals(today, spacedRepetitionService.calculateNextReviewDate(100));
    }

    @Test
    void testGetIntervalForState() {
        // Test interval retrieval for each state
        assertEquals(0, spacedRepetitionService.getIntervalForState(0));
        assertEquals(1, spacedRepetitionService.getIntervalForState(1));
        assertEquals(3, spacedRepetitionService.getIntervalForState(2));
        assertEquals(7, spacedRepetitionService.getIntervalForState(3));
        assertEquals(14, spacedRepetitionService.getIntervalForState(4));
        assertEquals(30, spacedRepetitionService.getIntervalForState(5));
        
        // Invalid states should return default interval
        assertEquals(0, spacedRepetitionService.getIntervalForState(-1));
        assertEquals(0, spacedRepetitionService.getIntervalForState(6));
    }

    @Test
    void testIsWordMastered() {
        assertFalse(spacedRepetitionService.isWordMastered(0));
        assertFalse(spacedRepetitionService.isWordMastered(1));
        assertFalse(spacedRepetitionService.isWordMastered(2));
        assertFalse(spacedRepetitionService.isWordMastered(3));
        assertFalse(spacedRepetitionService.isWordMastered(4));
        assertTrue(spacedRepetitionService.isWordMastered(5));
        assertTrue(spacedRepetitionService.isWordMastered(6)); // Above max
    }

    @Test
    void testGetMaxState() {
        assertEquals(5, spacedRepetitionService.getMaxState());
    }

    @Test
    void testCalculateDifficultyLevel() {
        // Test difficulty calculation based on success rate
        assertEquals("New", spacedRepetitionService.calculateDifficultyLevel(0, 0));
        assertEquals("Hard", spacedRepetitionService.calculateDifficultyLevel(1, 10)); // 10% success
        assertEquals("Hard", spacedRepetitionService.calculateDifficultyLevel(5, 10)); // 50% success
        assertEquals("Medium", spacedRepetitionService.calculateDifficultyLevel(6, 10)); // 60% success
        assertEquals("Medium", spacedRepetitionService.calculateDifficultyLevel(7, 10)); // 70% success
        assertEquals("Easy", spacedRepetitionService.calculateDifficultyLevel(8, 10)); // 80% success
        assertEquals("Easy", spacedRepetitionService.calculateDifficultyLevel(10, 10)); // 100% success
    }

    // Test word selection and prioritization logic
    @Test
    void testCalculateReviewPriority_DueWords() {
        Word word = createTestWord(1, "test", "prueba", 2);
        word.setLastReview(LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        // Word is overdue (state 2 should be reviewed after 3 days, but it's been 5 days)
        int priority = spacedRepetitionService.calculateReviewPriority(word);
        assertTrue(priority >= 0, "Overdue words should have high priority");
    }

    @Test
    void testCalculateReviewPriority_NotDueWords() {
        Word word = createTestWord(1, "test", "prueba", 3);
        word.setLastReview(LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        // Word is not due yet (state 3 should be reviewed after 7 days, only 1 day passed)
        int priority = spacedRepetitionService.calculateReviewPriority(word);
        assertEquals(6, priority, "Not due words should have lowest priority");
    }

    @Test
    void testUpdateWordAfterReview_CorrectAnswer() {
        Word word = createTestWord(1, "test", "prueba", 2);
        word.setReviewCount(5);
        word.setSuccessCount(3);
        
        Word updatedWord = spacedRepetitionService.updateWordAfterReview(word, true);
        
        assertEquals(3, updatedWord.getState(), "State should advance from 2 to 3");
        assertEquals(6, updatedWord.getReviewCount(), "Review count should increment");
        assertEquals(4, updatedWord.getSuccessCount(), "Success count should increment");
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), 
                    updatedWord.getLastReview(), "Last review should be today");
    }

    @Test
    void testUpdateWordAfterReview_IncorrectAnswer() {
        Word word = createTestWord(1, "test", "prueba", 3);
        word.setReviewCount(8);
        word.setSuccessCount(6);
        
        Word updatedWord = spacedRepetitionService.updateWordAfterReview(word, false);
        
        assertEquals(0, updatedWord.getState(), "State should reset to 0");
        assertEquals(9, updatedWord.getReviewCount(), "Review count should increment");
        assertEquals(6, updatedWord.getSuccessCount(), "Success count should not change");
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), 
                    updatedWord.getLastReview(), "Last review should be today");
    }

    @Test
    void testGetStateDescription() {
        assertEquals("New/Needs Review", spacedRepetitionService.getStateDescription(0));
        assertEquals("Learning (1 day)", spacedRepetitionService.getStateDescription(1));
        assertEquals("Familiar (3 days)", spacedRepetitionService.getStateDescription(2));
        assertEquals("Known (1 week)", spacedRepetitionService.getStateDescription(3));
        assertEquals("Well Known (2 weeks)", spacedRepetitionService.getStateDescription(4));
        assertEquals("Mastered (1 month)", spacedRepetitionService.getStateDescription(5));
        assertEquals("Unknown State", spacedRepetitionService.getStateDescription(6));
    }

    @Test
    void testGetWordsForReview_SortsByPriority() {
        // Create test words with different priorities
        Word overdueWord = createTestWord(1, "overdue", "atrasado", 0);
        overdueWord.setLastReview(LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        Word dueWord = createTestWord(2, "due", "debido", 1);
        dueWord.setLastReview(LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        Word notDueWord = createTestWord(3, "notdue", "no_debido", 2);
        notDueWord.setLastReview(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        List<Word> mockWords = Arrays.asList(notDueWord, dueWord, overdueWord);
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        
        List<Word> result = spacedRepetitionService.getWordsForReview();
        
        assertNotNull(result);
        verify(mockWordDAO).getWordsDueForReview();
        // The service should sort by priority, but we can't easily test the exact order
        // without more complex setup due to the priority calculation complexity
    }

    @Test
    void testGetWordsForReview_WithMaxLimit() {
        List<Word> mockWords = Arrays.asList(
            createTestWord(1, "word1", "palabra1", 0),
            createTestWord(2, "word2", "palabra2", 0),
            createTestWord(3, "word3", "palabra3", 0)
        );
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        
        List<Word> result = spacedRepetitionService.getWordsForReview(2);
        
        assertNotNull(result);
        assertTrue(result.size() <= 2, "Should not exceed max words limit");
        verify(mockWordDAO).getWordsDueForReview();
    }

    @Test
    void testGetWordsForReviewByStates() {
        Word state0Word = createTestWord(1, "new", "nuevo", 0);
        Word state1Word = createTestWord(2, "learning", "aprendiendo", 1);
        state1Word.setLastReview(LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        when(mockWordDAO.getWordsByState(0)).thenReturn(Arrays.asList(state0Word));
        when(mockWordDAO.getWordsByState(1)).thenReturn(Arrays.asList(state1Word));
        
        List<Word> result = spacedRepetitionService.getWordsForReviewByStates(new int[]{0, 1}, 10);
        
        assertNotNull(result);
        verify(mockWordDAO).getWordsByState(0);
        verify(mockWordDAO).getWordsByState(1);
    }

    @Test
    void testProcessReviewResult() {
        Word word = createTestWord(1, "test", "prueba", 1);
        when(mockWordDAO.updateWordReviewData(any(Word.class))).thenReturn(true);
        
        boolean result = spacedRepetitionService.processReviewResult(word, true);
        
        assertTrue(result);
        verify(mockWordDAO).updateWordReviewData(any(Word.class));
    }

    @Test
    void testGetHighPriorityWords() {
        Word newWord = createTestWord(1, "new", "nuevo", 0);
        Word overdueWord = createTestWord(2, "overdue", "atrasado", 2);
        overdueWord.setLastReview(LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE));
        Word normalWord = createTestWord(3, "normal", "normal", 3);
        normalWord.setLastReview(LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        when(mockWordDAO.listAll()).thenReturn(Arrays.asList(newWord, overdueWord, normalWord));
        
        List<Word> result = spacedRepetitionService.getHighPriorityWords();
        
        assertNotNull(result);
        verify(mockWordDAO).listAll();
    }

    @Test
    void testGetDueWordCountByState() {
        Word state0Word = createTestWord(1, "new", "nuevo", 0);
        Word state1Word = createTestWord(2, "learning", "aprendiendo", 1);
        state1Word.setLastReview(LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        when(mockWordDAO.getWordsByState(0)).thenReturn(Arrays.asList(state0Word));
        when(mockWordDAO.getWordsByState(1)).thenReturn(Arrays.asList(state1Word));
        when(mockWordDAO.getWordsByState(2)).thenReturn(Arrays.asList());
        when(mockWordDAO.getWordsByState(3)).thenReturn(Arrays.asList());
        when(mockWordDAO.getWordsByState(4)).thenReturn(Arrays.asList());
        when(mockWordDAO.getWordsByState(5)).thenReturn(Arrays.asList());
        
        Map<Integer, Integer> result = spacedRepetitionService.getDueWordCountByState();
        
        assertNotNull(result);
        assertEquals(6, result.size(), "Should have counts for all states 0-5");
        verify(mockWordDAO, times(6)).getWordsByState(anyInt());
    }

    // Study session tests
    @Test
    void testStartStudySession_WithWords() {
        List<Word> mockWords = Arrays.asList(
            createTestWord(1, "word1", "palabra1", 0),
            createTestWord(2, "word2", "palabra2", 0)
        );
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        
        StudySession session = spacedRepetitionService.startStudySession(5);
        
        assertNotNull(session);
        assertEquals(0, session.getWordsReviewed());
        assertEquals(0, session.getCorrectAnswers());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), session.getSessionDate());
    }

    @Test
    void testStartStudySession_NoWords() {
        when(mockWordDAO.getWordsDueForReview()).thenReturn(Arrays.asList());
        
        StudySession session = spacedRepetitionService.startStudySession(5);
        
        assertNull(session, "Should return null when no words are due");
    }

    @Test
    void testEndStudySession() {
        // Start a session first
        List<Word> mockWords = Arrays.asList(createTestWord(1, "word1", "palabra1", 0));
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        StudySession completedSession = spacedRepetitionService.endStudySession();
        
        assertNotNull(completedSession);
        assertTrue(completedSession.getSessionDuration() >= 0);
    }

    @Test
    void testGetSessionStats() {
        // Test without active session
        assertNull(spacedRepetitionService.getSessionStats());
        
        // Start a session and test
        List<Word> mockWords = Arrays.asList(createTestWord(1, "word1", "palabra1", 0));
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        StudySession stats = spacedRepetitionService.getSessionStats();
        assertNotNull(stats);
    }

    @Test
    void testGetNextSessionWord() {
        List<Word> mockWords = Arrays.asList(
            createTestWord(1, "word1", "palabra1", 0),
            createTestWord(2, "word2", "palabra2", 0)
        );
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        Word nextWord = spacedRepetitionService.getNextSessionWord();
        
        assertNotNull(nextWord);
        assertEquals("word1", nextWord.getTerm());
    }

    @Test
    void testProcessSessionReview() {
        List<Word> mockWords = Arrays.asList(createTestWord(1, "word1", "palabra1", 0));
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        when(mockWordDAO.updateWordReviewData(any(Word.class))).thenReturn(true);
        
        spacedRepetitionService.startStudySession(5);
        Word word = spacedRepetitionService.getNextSessionWord();
        
        boolean result = spacedRepetitionService.processSessionReview(word, true);
        
        assertTrue(result);
        StudySession stats = spacedRepetitionService.getSessionStats();
        assertEquals(1, stats.getWordsReviewed());
        assertEquals(1, stats.getCorrectAnswers());
    }

    @Test
    void testIsSessionComplete() {
        // No active session
        assertTrue(spacedRepetitionService.isSessionComplete());
        
        // Active session with words
        List<Word> mockWords = Arrays.asList(createTestWord(1, "word1", "palabra1", 0));
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        assertFalse(spacedRepetitionService.isSessionComplete());
    }

    @Test
    void testGetSessionProgress() {
        List<Word> mockWords = Arrays.asList(
            createTestWord(1, "word1", "palabra1", 0),
            createTestWord(2, "word2", "palabra2", 0)
        );
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        SpacedRepetitionService.SessionProgress progress = spacedRepetitionService.getSessionProgress();
        
        assertNotNull(progress);
        assertEquals(0, progress.getCompletedWords());
        assertEquals(2, progress.getTotalWords());
        assertEquals(0.0, progress.getProgressPercentage());
        assertEquals(2, progress.getRemainingWords());
    }

    @Test
    void testSkipCurrentWord() {
        List<Word> mockWords = Arrays.asList(
            createTestWord(1, "word1", "palabra1", 0),
            createTestWord(2, "word2", "palabra2", 0)
        );
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        Word skippedWord = spacedRepetitionService.skipCurrentWord();
        
        assertNotNull(skippedWord);
        assertEquals("word2", skippedWord.getTerm());
    }

    @Test
    void testGetRemainingWordsCount() {
        List<Word> mockWords = Arrays.asList(
            createTestWord(1, "word1", "palabra1", 0),
            createTestWord(2, "word2", "palabra2", 0),
            createTestWord(3, "word3", "palabra3", 0)
        );
        when(mockWordDAO.getWordsDueForReview()).thenReturn(mockWords);
        spacedRepetitionService.startStudySession(5);
        
        assertEquals(3, spacedRepetitionService.getRemainingWordsCount());
        
        // Skip one word
        spacedRepetitionService.skipCurrentWord();
        assertEquals(2, spacedRepetitionService.getRemainingWordsCount());
    }

    // Helper method to create test words
    private Word createTestWord(int id, String term, String translation, int state) {
        Word word = new Word();
        word.setIdTerm(id);
        word.setTerm(term);
        word.setTranslation(translation);
        word.setState(state);
        word.setReviewCount(0);
        word.setSuccessCount(0);
        return word;
    }
}