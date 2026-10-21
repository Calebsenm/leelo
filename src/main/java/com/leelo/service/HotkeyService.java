package com.leelo.service;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The keyboard shortcuts of the reading screen.
 *
 * <p>Every action has a default shortcut that the user can change; the
 * shortcuts are stored as settings, using the same names as the JavaFX key
 * combinations.
 */
public class HotkeyService {

    /** The category a shortcut belongs to, used to group them in the settings. */
    public enum Category {
        NAVIGATION("Navigation"),
        STATUS("Update status"),
        PAGING("Paging"),
        TRANSLATE("Translate"),
        COPY("Copy"),
        MISC("Misc");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** The actions that can be triggered from the reading screen. */
    public enum Action {
        START_HOVER(Category.NAVIGATION, "Deselect all words", "Escape"),
        PREV_WORD(Category.NAVIGATION, "Move to the previous word", "Left"),
        NEXT_WORD(Category.NAVIGATION, "Move to the next word", "Right"),
        PREV_UNKNOWN_WORD(Category.NAVIGATION, "Move to the previous unknown word", ""),
        NEXT_UNKNOWN_WORD(Category.NAVIGATION, "Move to the next unknown word", ""),
        PREV_SENTENCE(Category.NAVIGATION, "Move to the previous sentence", ""),
        NEXT_SENTENCE(Category.NAVIGATION, "Move to the next sentence", ""),
        STATUS_1(Category.STATUS, "Set the status to 1", "Digit1"),
        STATUS_2(Category.STATUS, "Set the status to 2", "Digit2"),
        STATUS_3(Category.STATUS, "Set the status to 3", "Digit3"),
        STATUS_4(Category.STATUS, "Set the status to 4", "Digit4"),
        STATUS_5(Category.STATUS, "Set the status to 5", "Digit5"),
        STATUS_IGNORE(Category.STATUS, "Set the status to Ignored", "I"),
        STATUS_WELL_KNOWN(Category.STATUS, "Set the status to Well Known", "W"),
        STATUS_UP(Category.STATUS, "Move the status up", "Up"),
        STATUS_DOWN(Category.STATUS, "Move the status down", "Down"),
        DELETE_TERM(Category.STATUS, "Delete the term", ""),
        PREVIOUS_PAGE(Category.PAGING, "Go to the previous page", ""),
        NEXT_PAGE(Category.PAGING, "Go to the next page", ""),
        MARK_READ(Category.PAGING, "Mark the page as read and go to the next one", ""),
        MARK_READ_WELL_KNOWN(Category.PAGING, "Mark the remaining words as known, then go to the next page", ""),
        TRANSLATE_SENTENCE(Category.TRANSLATE, "Translate the sentence of the current word", "T"),
        TRANSLATE_PARAGRAPH(Category.TRANSLATE, "Translate the paragraph of the current word", "Shift+T"),
        TRANSLATE_PAGE(Category.TRANSLATE, "Translate the whole page", ""),
        COPY_SENTENCE(Category.COPY, "Copy the sentence of the current word", "C"),
        COPY_PARAGRAPH(Category.COPY, "Copy the paragraph of the current word", "Shift+C"),
        COPY_PAGE(Category.COPY, "Copy the whole page", ""),
        PAGE_TERM_LIST(Category.MISC, "Show the terms of the current page", ""),
        BOOKMARK(Category.MISC, "Bookmark the current page", "B"),
        EDIT_PAGE(Category.MISC, "Edit the current page", ""),
        TOGGLE_HIGHLIGHT(Category.MISC, "Toggle the highlights", "H"),
        TOGGLE_FOCUS(Category.MISC, "Toggle the focus mode", "F"),
        SAVE_TERM(Category.MISC, "Save the term", "Ctrl+Enter");

        private final Category category;
        private final String description;
        private final String defaultCombination;

        Action(Category category, String description, String defaultCombination) {
            this.category = category;
            this.description = description;
            this.defaultCombination = defaultCombination;
        }

        public Category getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultCombination() {
            return defaultCombination;
        }

        /** The name of the setting holding the shortcut of this action. */
        public String getSettingKey() {
            return "hotkey_" + name();
        }
    }

    private final SettingService settingService;

    /** Names that JavaFX does not understand but the settings may contain. */
    private static final Map<String, KeyCode> KEY_ALIASES = keyAliases();

    private static Map<String, KeyCode> keyAliases() {
        Map<String, KeyCode> aliases = new java.util.HashMap<>();
        aliases.put("ESCAPE", KeyCode.ESCAPE);
        aliases.put("PAGEUP", KeyCode.PAGE_UP);
        aliases.put("PAGE_UP", KeyCode.PAGE_UP);
        aliases.put("PAGEDOWN", KeyCode.PAGE_DOWN);
        aliases.put("PAGE_DOWN", KeyCode.PAGE_DOWN);
        for (int i = 0; i <= 9; i++) {
            aliases.put("DIGIT" + i, KeyCode.getKeyCode(Integer.toString(i)));
        }
        return Map.copyOf(aliases);
    }

    public HotkeyService(SettingService settingService) {
        this.settingService = settingService;
    }

    /** Returns the shortcut currently assigned to an action. */
    public KeyCombination getCombination(Action action) {
        String stored = settingService.getRaw(action.getSettingKey());
        if (stored == null || stored.isBlank()) {
            stored = action.getDefaultCombination();
        }
        return fromName(stored);
    }

    /** Assigns a shortcut to an action; an empty value disables the shortcut. */
    public void setCombination(Action action, KeyCombination combination) {
        settingService.setRaw(action.getSettingKey(),
                combination == null ? "" : toDisplayName(combination));
    }

    /**
     * Returns the readable name of a shortcut.
     *
     * <p>The name is built by hand instead of using
     * {@link KeyCombination#getName()}, because JavaFX writes some keys in a
     * cryptic way ({@code Esc}, {@code 1}) and quotes the character
     * combinations. The result can be read back by {@link #fromName(String)}.
     */
    public static String toDisplayName(KeyCombination combination) {
        if (combination == null) {
            return "";
        }
        if (!(combination instanceof KeyCodeCombination code)) {
            return unquote(combination.getName());
        }
        StringBuilder name = new StringBuilder();
        if (isDown(code.getControl())) {
            name.append("Ctrl+");
        }
        if (isDown(code.getShift())) {
            name.append("Shift+");
        }
        if (isDown(code.getAlt())) {
            name.append("Alt+");
        }
        if (isDown(code.getMeta())) {
            name.append("Meta+");
        }
        name.append(displayNameOf(code.getCode()));
        return name.toString();
    }

    /** True when a modifier is pressed as part of the combination. */
    private static boolean isDown(KeyCombination.ModifierValue value) {
        return value == KeyCombination.ModifierValue.DOWN;
    }

    /** Returns the readable name of a key of the keyboard. */
    private static String displayNameOf(KeyCode keyCode) {
        if (keyCode == null) {
            return "";
        }
        if (keyCode == KeyCode.ESCAPE) {
            return "Escape";
        }
        if (keyCode == KeyCode.DELETE) {
            return "Delete";
        }
        if (keyCode.isDigitKey()) {
            return "Digit" + keyCode.getChar();
        }
        return keyCode.getName();
    }

    /**
     * Builds a shortcut out of its name.
     *
     * <p>The name can be a key of the keyboard ({@code Escape}, {@code Digit1},
     * {@code I}), a combination of modifiers ({@code Ctrl+Enter}) or a single
     * character ({@code t}).
     */
    public static KeyCombination fromName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = unquote(name.trim());
        if (trimmed.isEmpty()) {
            return null;
        }
        KeyCode code = keyCodeOf(trimmed);
        if (code != null) {
            return new KeyCodeCombination(code);
        }
        if (!trimmed.contains("+")) {
            return null;
        }

        List<KeyCombination.Modifier> modifiers = new ArrayList<>();
        String[] parts = trimmed.split("\\+");
        for (int i = 0; i < parts.length - 1; i++) {
            KeyCombination.Modifier modifier = modifierOf(parts[i].trim());
            if (modifier == null) {
                return null;
            }
            modifiers.add(modifier);
        }
        KeyCode modifierKey = keyCodeOf(parts[parts.length - 1].trim());
        if (modifierKey == null || modifiers.isEmpty()) {
            return null;
        }
        return new KeyCodeCombination(modifierKey,
                modifiers.toArray(new KeyCombination.Modifier[0]));
    }

    /** Returns the key of the keyboard with the given name, or {@code null}. */
    private static KeyCode keyCodeOf(String name) {
        KeyCode code = KeyCode.getKeyCode(name);
        if (code != null) {
            return code;
        }
        KeyCode alias = KEY_ALIASES.get(name.toUpperCase(Locale.ROOT));
        if (alias != null) {
            return alias;
        }
        if (name.length() == 1) {
            return KeyCode.getKeyCode(name.toUpperCase(Locale.ROOT));
        }
        return null;
    }

    /** Returns the modifier with the given name, or {@code null}. */
    private static KeyCombination.Modifier modifierOf(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "ctrl", "control" -> KeyCombination.CONTROL_DOWN;
            case "shift" -> KeyCombination.SHIFT_DOWN;
            case "alt", "option" -> KeyCombination.ALT_DOWN;
            case "meta", "cmd", "command", "win", "super" -> KeyCombination.META_DOWN;
            default -> null;
        };
    }

    /** Removes the quotes JavaFX wraps around the named keys. */
    private static String unquote(String name) {
        if (name.length() >= 2 && name.startsWith("'") && name.endsWith("'")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    /** True when the given key event triggers the action. */
    public boolean matches(Action action, KeyEvent event) {
        KeyCombination combination = getCombination(action);
        return combination != null && combination.match(event);
    }

    /**
     * Returns the action the given key event triggers, or {@code null} when the
     * key is not bound to anything.
     */
    public Action match(KeyEvent event) {
        for (Action action : Action.values()) {
            if (matches(action, event)) {
                return action;
            }
        }
        return null;
    }

    /** Returns the shortcuts that are already assigned to another action. */
    public List<Action> findConflicts(Action action, KeyCombination combination) {
        List<Action> conflicts = new ArrayList<>();
        if (combination == null) {
            return conflicts;
        }
        for (Action other : Action.values()) {
            if (other == action) {
                continue;
            }
            KeyCombination existing = getCombination(other);
            if (existing != null && existing.getName().equals(combination.getName())) {
                conflicts.add(other);
            }
        }
        return conflicts;
    }

    /** Returns the shortcuts grouped by category, for the settings screen. */
    public Map<Category, List<Action>> getGroupedActions() {
        Map<Category, List<Action>> grouped = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            grouped.put(category, new ArrayList<>());
        }
        for (Action action : Action.values()) {
            grouped.get(action.getCategory()).add(action);
        }
        return grouped;
    }
}
