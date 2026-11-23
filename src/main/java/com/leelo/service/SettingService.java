package com.leelo.service;

import com.leelo.dao.SettingDAO;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Access to the settings of the user.
 *
 * <p>The settings are stored as text; this class gives them a type and a
 * default value, so that the rest of the application never has to deal with
 * the raw representation.
 */
public class SettingService {

    /** The language the application is currently working with; zero means "all". */
    public static final String CURRENT_LANGUAGE_ID = "current_language_id";

    /** Whether the terms are highlighted with their status colour. */
    public static final String SHOW_HIGHLIGHTS = "show_highlights";

    /** Whether the translation of the parent is promoted in the term popup. */
    public static final String PROMOTE_PARENT_TRANSLATION = "term_popup_promote_parent_translation";

    /** Whether the components of a term are shown in the term popup. */
    public static final String SHOW_TERM_COMPONENTS = "term_popup_show_components";

    /** Whether the reading streak is shown on the home screen. */
    public static final String SHOW_STREAK_ON_HOME = "show_streak_on_home";

    /** Number of pages sampled when the statistics of a book are calculated. */
    public static final String STATS_SAMPLE_SIZE = "stats_calc_sample_size";

    /** Font size of the reading screen. */
    public static final String FONT_SIZE = "font_size";

    /** Line height of the reading screen. */
    public static final String LINE_SPACING = "line_spacing";

    /** Width of the text column of the reading screen, in pixels. */
    public static final String READING_WIDTH = "reading_width";

    /** URL of the Ollama server. */
    public static final String OLLAMA_URL = "ollama_url";

    /** Model name to use with Ollama. */
    public static final String OLLAMA_MODEL = "ollama_model";

    /** Whether to automatically translate untranslated words on click with Ollama. */
    public static final String OLLAMA_AUTO_TRANSLATE = "ollama_auto_translate";

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(CURRENT_LANGUAGE_ID, "0");
        DEFAULTS.put(SHOW_HIGHLIGHTS, "true");
        DEFAULTS.put(PROMOTE_PARENT_TRANSLATION, "true");
        DEFAULTS.put(SHOW_TERM_COMPONENTS, "true");
        DEFAULTS.put(SHOW_STREAK_ON_HOME, "false");
        DEFAULTS.put(STATS_SAMPLE_SIZE, "5");
        DEFAULTS.put(FONT_SIZE, "25");
        DEFAULTS.put(LINE_SPACING, "1.5");
        DEFAULTS.put(READING_WIDTH, "760");
        DEFAULTS.put(OLLAMA_URL, "http://localhost:11434");
        DEFAULTS.put(OLLAMA_MODEL, "qwen2.5:3b");
        DEFAULTS.put(OLLAMA_AUTO_TRANSLATE, "false");
    }

    private final SettingDAO settingDAO = new SettingDAO();

    /** Stores the default value of every setting that has none yet. */
    public void initializeDefaults() {
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (!settingDAO.exists(entry.getKey())) {
                settingDAO.setValue(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Returns the raw value of a setting. */
    public String getRaw(String key) {
        String value = settingDAO.getValue(key);
        return value == null ? DEFAULTS.get(key) : value;
    }

    /** Stores the raw value of a setting. */
    public void setRaw(String key, String value) {
        settingDAO.setValue(key, value);
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getRaw(key));
    }

    public void setBoolean(String key, boolean value) {
        setRaw(key, Boolean.toString(value));
    }

    public int getInt(String key) {
        try {
            return Integer.parseInt(getRaw(key).trim());
        } catch (NumberFormatException | NullPointerException e) {
            String fallback = DEFAULTS.get(key);
            return fallback == null ? 0 : Integer.parseInt(fallback);
        }
    }

    public void setInt(String key, int value) {
        setRaw(key, Integer.toString(value));
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(getRaw(key).trim());
        } catch (NumberFormatException | NullPointerException e) {
            String fallback = DEFAULTS.get(key);
            return fallback == null ? 0 : Double.parseDouble(fallback);
        }
    }

    public void setDouble(String key, double value) {
        setRaw(key, Double.toString(value));
    }

    /** The language the application is working with; zero means "all languages". */
    public int getCurrentLanguageId() {
        return getInt(CURRENT_LANGUAGE_ID);
    }

    public void setCurrentLanguageId(int languageId) {
        setInt(CURRENT_LANGUAGE_ID, languageId);
    }

    /** Returns every setting of the user, with the default values filled in. */
    public Map<String, String> getAll() {
        Map<String, String> settings = new LinkedHashMap<>(DEFAULTS);
        settings.putAll(settingDAO.findAllUserSettings());
        return settings;
    }
}
