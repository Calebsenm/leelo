package com.leelo.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the statuses of the terms.
 */
class TermStatusTest {

    @Test
    void resolvesTheStatusFromItsCode() {
        assertEquals(TermStatus.UNKNOWN, TermStatus.fromCode(0));
        assertEquals(TermStatus.LEARNED, TermStatus.fromCode(5));
        assertEquals(TermStatus.IGNORED, TermStatus.fromCode(98));
        assertEquals(TermStatus.WELL_KNOWN, TermStatus.fromCode(99));
    }

    @Test
    void resolvesTheStatusFromItsAbbreviation() {
        assertEquals(TermStatus.WELL_KNOWN, TermStatus.fromAbbreviation("W"));
        assertEquals(TermStatus.IGNORED, TermStatus.fromAbbreviation("i"));
        assertEquals(TermStatus.NEW_1, TermStatus.fromAbbreviation("1"));
        assertNull(TermStatus.fromAbbreviation("x"));
    }

    @Test
    void stepsUpAndDownTheLearningLadder() {
        assertEquals(TermStatus.NEW_2, TermStatus.NEW_1.stepUp());
        assertEquals(TermStatus.WELL_KNOWN, TermStatus.LEARNED.stepUp());
        assertEquals(TermStatus.LEARNING_3, TermStatus.LEARNING_4.stepDown());
    }

    @Test
    void doesNotStepPastTheEndsOfTheLadder() {
        assertNull(TermStatus.WELL_KNOWN.stepUp());
        assertNull(TermStatus.UNKNOWN.stepDown());
    }

    @Test
    void leavesTheLadderWhenTheTermIsIgnored() {
        assertEquals(TermStatus.UNKNOWN, TermStatus.IGNORED.stepUp());
        assertEquals(TermStatus.UNKNOWN, TermStatus.IGNORED.stepDown());
    }

    @Test
    void highlightsOnlyTheWordsThatAreStillBeingLearned() {
        assertTrue(TermStatus.UNKNOWN.isHighlighted());
        assertTrue(TermStatus.NEW_1.isHighlighted());
        assertTrue(TermStatus.NEW_2.isHighlighted());
        assertTrue(TermStatus.LEARNING_3.isHighlighted());
        assertTrue(TermStatus.LEARNING_4.isHighlighted());
        assertFalse(TermStatus.LEARNED.isHighlighted());
        assertFalse(TermStatus.IGNORED.isHighlighted());
        assertFalse(TermStatus.WELL_KNOWN.isHighlighted());
    }
}
