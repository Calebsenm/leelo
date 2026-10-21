package com.leelo.render;

import com.leelo.model.Term;
import com.leelo.util.TextTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * A unit of rendered text.
 *
 * <p>A text item covers one or more tokens of the parsed page. When a longer
 * term overlaps a shorter one, the shorter item is partially hidden: only its
 * trailing tokens remain visible, which is what {@link #getDisplayText()}
 * returns.
 */
public class TextItem {

    private int index;
    private int languageId;
    private String text = "";
    private String textLc = "";
    private boolean word;
    private int tokenCount = 1;
    private int displayCount = 1;
    private int sentenceNumber;
    private int paragraphNumber;
    private Term term;
    private final List<String> extraCssClasses = new ArrayList<>();

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    /** The original text of the item, with the tokens joined by zero-width spaces. */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    /** The lower-cased counterpart of {@link #getText()}. */
    public String getTextLc() {
        return textLc;
    }

    public void setTextLc(String textLc) {
        this.textLc = textLc == null ? "" : textLc;
    }

    /** True when the item corresponds to a term. */
    public boolean isWord() {
        return word;
    }

    public void setWord(boolean word) {
        this.word = word;
    }

    /** Number of tokens the item originally covers. */
    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    /** Number of tokens actually displayed, counted from the end of the item. */
    public int getDisplayCount() {
        return displayCount;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public int getSentenceNumber() {
        return sentenceNumber;
    }

    public void setSentenceNumber(int sentenceNumber) {
        this.sentenceNumber = sentenceNumber;
    }

    public int getParagraphNumber() {
        return paragraphNumber;
    }

    public void setParagraphNumber(int paragraphNumber) {
        this.paragraphNumber = paragraphNumber;
    }

    public Term getTerm() {
        return term;
    }

    /** Setting the term also records the language and the status of the item. */
    public void setTerm(Term term) {
        this.term = term;
        if (term != null) {
            this.languageId = term.getLanguageId();
        }
    }

    /** Id of the term behind the item, or zero when the item is not a term. */
    public int getTermId() {
        return term == null ? 0 : term.getId();
    }

    /** Status of the term behind the item, or {@code null} when there is none. */
    public Integer getStatus() {
        return term == null ? null : term.getStatus().getCode();
    }

    public List<String> getExtraCssClasses() {
        return extraCssClasses;
    }

    /** Adds an extra style class to the item. */
    public void addCssClass(String cssClass) {
        extraCssClasses.add(cssClass);
    }

    /** The visible text: the trailing {@link #getDisplayCount()} tokens of the item. */
    public String getDisplayText() {
        String[] tokens = TextTokens.split(text);
        if (tokens.length == 0) {
            return "";
        }
        int count = Math.min(displayCount, tokens.length);
        return TextTokens.join(List.of(tokens).subList(tokens.length - count, tokens.length));
    }

    /** The visible text, without the zero-width separators. */
    public String getPlainDisplayText() {
        return TextTokens.strip(getDisplayText());
    }

    /** Unique identifier of the item, used to anchor the user interface. */
    public String getSpanId() {
        return "ID-" + sentenceNumber + "-" + index;
    }

    /** The style class describing the status of the term behind the item. */
    public String getStatusClass() {
        if (term == null) {
            return "status0";
        }
        return "status" + term.getStatus().getCode();
    }

    /** True when part of the item is hidden behind a longer overlapping term. */
    public boolean isOverlapped() {
        return !getDisplayText().equals(text);
    }

    /** The full style class string of the item. */
    public String getCssClassString() {
        if (!word) {
            return "textitem";
        }
        StringBuilder builder = new StringBuilder("textitem click word word").append(getTermId());
        if (isOverlapped()) {
            builder.append(" overlapped");
        }
        for (String cssClass : extraCssClasses) {
            builder.append(' ').append(cssClass);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "<TextItem \"" + getPlainDisplayText() + "\" (term=" + getTermId() + ", sentence=" + sentenceNumber + ")>";
    }
}
