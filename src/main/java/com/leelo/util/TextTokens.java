package com.leelo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helpers for the zero-width-space separated token representation used to store
 * terms and sentences.
 *
 * <p>Joining tokens with a zero-width space keeps the original word boundaries
 * without introducing visible characters, and it makes multi-word terms
 * matchable against a text token by token.
 */
public final class TextTokens {

    /** The zero-width space used to join tokens. */
    public static final String ZWS = "\u200B";

    private static final Pattern ZWS_PATTERN = Pattern.compile(Pattern.quote(ZWS));

    private TextTokens() {
    }

    /** Joins tokens with the zero-width space. */
    public static String join(List<String> tokens) {
        return String.join(ZWS, tokens);
    }

    /** Splits a zero-width-space joined string back into its tokens. */
    public static String[] split(String joined) {
        if (joined == null || joined.isEmpty()) {
            return new String[0];
        }
        return ZWS_PATTERN.split(joined, -1);
    }

    /** Removes every zero-width space from the given text. */
    public static String strip(String text) {
        return text == null ? "" : ZWS_PATTERN.matcher(text).replaceAll("");
    }

    /** Counts the tokens of a zero-width-space joined string. */
    public static int countTokens(String joined) {
        if (joined == null || joined.isEmpty()) {
            return 0;
        }
        return split(joined).length;
    }

    /** Returns the tokens of a zero-width-space joined string as a list. */
    public static List<String> tokenList(String joined) {
        List<String> tokens = new ArrayList<>();
        for (String token : split(joined)) {
            tokens.add(token);
        }
        return tokens;
    }
}
