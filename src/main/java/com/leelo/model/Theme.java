package com.leelo.model;

import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;

/**
 * A theme of the reading screen.
 *
 * <p>A theme defines the colour of every term status, together with the colours
 * of the page itself. It plays the same role as the stylesheets of Lute, which
 * define the colours through the {@code --status-N-color} custom properties.
 */
public class Theme {

    private final String name;
    private final Map<TermStatus, Color> statusColors = new EnumMap<>(TermStatus.class);
    private Color background = Color.WHITE;
    private Color foreground = Color.BLACK;
    private Color accent = Color.web("#1976D2");

    public Theme(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<TermStatus, Color> getStatusColors() {
        return statusColors;
    }

    public Theme setStatusColor(TermStatus status, Color color) {
        statusColors.put(status, color);
        return this;
    }

    /** The colour of a status, falling back to the colour of the unknown status. */
    public Color getStatusColor(TermStatus status) {
        Color color = statusColors.get(status);
        return color != null ? color : statusColors.get(TermStatus.UNKNOWN);
    }

    public Color getBackground() {
        return background;
    }

    public Theme setBackground(Color background) {
        this.background = background;
        return this;
    }

    public Color getForeground() {
        return foreground;
    }

    public Theme setForeground(Color foreground) {
        this.foreground = foreground;
        return this;
    }

    public Color getAccent() {
        return accent;
    }

    public Theme setAccent(Color accent) {
        this.accent = accent;
        return this;
    }

    /** True when the theme is meant for a dark background. */
    public boolean isDark() {
        return background.getBrightness() < 0.5;
    }

    @Override
    public String toString() {
        return name;
    }
}
