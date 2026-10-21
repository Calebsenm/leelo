package com.leelo.service;

import com.leelo.model.TermStatus;
import com.leelo.model.Theme;
import javafx.scene.paint.Color;

/**
 * The colours of the reading screen.
 *
 * <p>There is a single palette, copied from the default theme of Lute v3, so
 * that a reader of both applications finds exactly the same colours. The only
 * user choice is whether the colours are shown or not.
 */
public class ThemeService {

    /** The name of the palette. */
    public static final String THEME_NAME = "Lute";

    private static final Theme LUTE = luteTheme();

    private final SettingService settingService;

    public ThemeService(SettingService settingService) {
        this.settingService = settingService;
    }

    /** Returns the colours of the reading screen. */
    public Theme getCurrentTheme() {
        return LUTE;
    }

    /** True when the terms are highlighted with their status colour. */
    public boolean isHighlightingEnabled() {
        return settingService.getBoolean(SettingService.SHOW_HIGHLIGHTS);
    }

    /** Turns the highlighting of the terms on or off. */
    public boolean toggleHighlighting() {
        boolean enabled = !isHighlightingEnabled();
        settingService.setBoolean(SettingService.SHOW_HIGHLIGHTS, enabled);
        return enabled;
    }

    /**
     * The default palette of Lute v3, taken from its {@code styles.css}.
     *
     * <p>Statuses 0 to 5 are shared with Lute; 98 (ignored) and 99 (well
     * known) are the two special statuses.
     */
    private static Theme luteTheme() {
        return new Theme(THEME_NAME)
                .setStatusColor(TermStatus.UNKNOWN, Color.web("#D5FFFF"))
                .setStatusColor(TermStatus.NEW_1, Color.web("#F5B8A9"))
                .setStatusColor(TermStatus.NEW_2, Color.web("#F5CCA9"))
                .setStatusColor(TermStatus.LEARNING_3, Color.web("#F5E1A9"))
                .setStatusColor(TermStatus.LEARNING_4, Color.web("#F5F3A9"))
                .setStatusColor(TermStatus.LEARNED, Color.web("#DDFFDD"))
                .setStatusColor(TermStatus.IGNORED, Color.web("#EE8577"))
                .setStatusColor(TermStatus.WELL_KNOWN, Color.web("#72DA88"))
                .setBackground(Color.WHITE)
                .setForeground(Color.BLACK)
                .setAccent(Color.web("#1976D2"));
    }
}
