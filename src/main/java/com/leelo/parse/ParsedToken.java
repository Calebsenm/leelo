package com.leelo.parse;

/**
 * A single token produced by a {@link TextParser}.
 *
 * <p>Tokens are either words (they can be turned into terms) or plain text such
 * as punctuation and whitespace.
 */
public class ParsedToken {

    /** Marker token inserted between paragraphs. */
    public static final String PARAGRAPH_MARKER = "¶";

    private final String token;
    private final boolean word;
    private final boolean endOfSentence;
    private final int order;
    private final int sentenceNumber;

    public ParsedToken(String token, boolean word, boolean endOfSentence, int order, int sentenceNumber) {
        this.token = token;
        this.word = word;
        this.endOfSentence = endOfSentence;
        this.order = order;
        this.sentenceNumber = sentenceNumber;
    }

    public String getToken() {
        return token;
    }

    /** True when the token is a word and can therefore become a term. */
    public boolean isWord() {
        return word;
    }

    /** True when the token closes a sentence. */
    public boolean isEndOfSentence() {
        return endOfSentence;
    }

    /** True when the token is the paragraph marker. */
    public boolean isEndOfParagraph() {
        return PARAGRAPH_MARKER.equals(token.trim());
    }

    /** Position of the token inside the parsed text. */
    public int getOrder() {
        return order;
    }

    /** Zero-based number of the sentence the token belongs to. */
    public int getSentenceNumber() {
        return sentenceNumber;
    }

    @Override
    public String toString() {
        return token;
    }
}
