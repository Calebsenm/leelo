package com.leelo.service;

import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests of the keyboard shortcuts.
 */
class HotkeyServiceTest {

    @Test
    void everyDefaultShortcutIsValid() {
        for (HotkeyService.Action action : HotkeyService.Action.values()) {
            String combination = action.getDefaultCombination();
            if (combination.isEmpty()) {
                continue;
            }
            assertNotNull(HotkeyService.fromName(combination), "Invalid shortcut for " + action);
        }
    }

    @Test
    void removesTheQuotesOfTheNamedKeys() {
        KeyCombination combination = HotkeyService.fromName("Escape");

        assertNotNull(combination);
        assertEquals("Escape", HotkeyService.toDisplayName(combination));
    }

    @Test
    void readsTheQuotedNames() {
        assertNotNull(HotkeyService.fromName("'Escape'"));
        assertNotNull(HotkeyService.fromName("Escape"));
    }

    @Test
    void readsTheCombinationsWithModifiers() {
        KeyCombination combination = HotkeyService.fromName("Ctrl+Enter");

        assertNotNull(combination);
        assertEquals("Ctrl+Enter", HotkeyService.toDisplayName(combination));
    }

    @Test
    void hasNoShortcutForAnEmptyName() {
        assertNull(HotkeyService.fromName(""));
        assertNull(HotkeyService.fromName(null));
        assertNull(HotkeyService.fromName("not a shortcut"));
    }
}
