package com.leelo.render;

import java.util.ArrayList;
import java.util.List;

/** A group of sentences forming a single paragraph. */
public class RenderedParagraph {

    private final int paragraphNumber;
    private final List<RenderedSentence> sentences = new ArrayList<>();

    public RenderedParagraph(int paragraphNumber) {
        this.paragraphNumber = paragraphNumber;
    }

    public int getParagraphNumber() {
        return paragraphNumber;
    }

    public List<RenderedSentence> getSentences() {
        return sentences;
    }

    /** The visible text of the paragraph. */
    public String getPlainText() {
        StringBuilder builder = new StringBuilder();
        for (RenderedSentence sentence : sentences) {
            builder.append(sentence.getPlainText()).append(' ');
        }
        return builder.toString().trim();
    }
}
