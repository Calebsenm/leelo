package com.leelo.view;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * A piece of text that can be given a background, laid out inline.
 *
 * <p>The reading screen highlights every term with the colour of its status.
 * A plain {@link Text} cannot carry a background, so the words are laid out
 * with this component instead: the text flow treats it as a single, unbreakable
 * unit, exactly like the inline elements of a document.
 */
public class InlineText extends Region {

    private final Text text;

    /**
     * Creates a piece of text.
     *
     * @param content   the text to display
     * @param font      the font to use
     * @param fill      the colour of the text
     * @param background the colour of the background, may be {@code null}
     */
    public InlineText(String content, Font font, Color fill, Color background) {
        text = new Text(content);
        text.setFont(font);
        text.setFill(fill);
        text.setMouseTransparent(true);

        setPadding(Insets.EMPTY);
        setBackground(background == null
                ? null
                : new Background(new BackgroundFill(background, null, Insets.EMPTY)));
        getChildren().add(text);
    }

    /** The underlying text node. */
    public Text getTextNode() {
        return text;
    }

    /** Changes the colour of the text. */
    public void setFill(Color fill) {
        text.setFill(fill);
    }

    /** Changes the colour of the background. */
    public void setBackgroundColor(Color background) {
        setBackground(background == null
                ? null
                : new Background(new BackgroundFill(background, null, Insets.EMPTY)));
    }

    /** Adds or removes the underline used to mark the word under the mouse. */
    public void setUnderlined(boolean underlined) {
        text.setUnderline(underlined);
    }

    @Override
    protected void layoutChildren() {
        double width = text.prefWidth(-1);
        double height = text.prefHeight(-1);
        text.resizeRelocate(0, 0, width, height);
    }

    @Override
    protected double computePrefWidth(double height) {
        return text.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return text.prefHeight(width);
    }

    @Override
    public double getBaselineOffset() {
        return text.getBaselineOffset();
    }
}
