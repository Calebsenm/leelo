package com.leelo.parse;

import com.leelo.model.Language;

import java.util.List;

/**
 * Turns a raw text into the sequence of tokens used by the reading screen.
 *
 * <p>Implementations are stateless and can be shared freely.
 */
public interface TextParser {

    /** Stable identifier of the parser, stored on the language. */
    String getType();

    /** Human readable name shown in the user interface. */
    String getName();

    /**
     * Parses the text into tokens.
     *
     * @param text     the raw text
     * @param language the language whose settings drive the parsing
     */
    List<ParsedToken> getParsedTokens(String text, Language language);

    /**
     * Returns the reading (pronunciation) of a term, or an empty string when the
     * parser cannot produce one.
     */
    default String getReading(String text, Language language) {
        return "";
    }

    /** Lower-cases the text according to the rules of the language. */
    default String getLowercase(String text, Language language) {
        return text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
    }

    /** True when the parser can be used on this machine. */
    default boolean isSupported() {
        return true;
    }
}
