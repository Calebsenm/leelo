package com.leelo.render;

import java.util.ArrayList;
import java.util.List;

/** A group of text items forming a single sentence. */
public class RenderedSentence {

    private final int sentenceNumber;
    private final List<TextItem> items = new ArrayList<>();

    public RenderedSentence(int sentenceNumber) {
        this.sentenceNumber = sentenceNumber;
    }

    public int getSentenceNumber() {
        return sentenceNumber;
    }

    public List<TextItem> getItems() {
        return items;
    }

    /** The visible text of the sentence. */
    public String getPlainText() {
        StringBuilder builder = new StringBuilder();
        for (TextItem item : items) {
            builder.append(item.getPlainDisplayText());
        }
        return builder.toString();
    }
}
