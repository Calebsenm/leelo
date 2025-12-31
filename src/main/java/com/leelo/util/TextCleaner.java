package com.leelo.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TextCleaner {

    /**
     * Cleans the input text by removing unwanted artifacts and normalizing
     * formatting.
     * 
     * @param text The raw text to clean.
     * @return The cleaned text.
     */
    public static String clean(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 1. Fix spaced characters (e.g., "h o l a" -> "hola")
        // Loop ensures we handle overlapping or long sequences correctly until no more
        // matches are found.
        String previous;
        do {
            previous = text;
            text = text.replaceAll("(?<=\\b[a-zA-Z\\u00C0-\\u00FF])[ ](?=[a-zA-Z\\u00C0-\\u00FF]\\b)", "");
        } while (!text.equals(previous));

        // 2. Remove references like [1], [12], [a]
        text = text.replaceAll("\\[\\w+\\]", "");

        // 3. Remove standalone symbols that are likely noise (#, *, etc)
        // We keep basic punctuation .,;?!()''""-
        // Remove lines that are just symbols

        // Remove specific unwanted chars mentioned: #
        text = text.replace("#", "");
        // Remove command symbol and other common UI artifacts if needed
        text = text.replace("⌘", "");

        // 5. Extreme Cases Handling
        // Encoding errors (Replacement Character)
        text = text.replace("\uFFFD", "");

        // Invisible characters and Control Characters
        // Remove Zero Width Space (\u200B), BOM (\uFEFF)
        text = text.replace("\u200B", "").replace("\uFEFF", "");
        // Remove control characters (except newline \n, return \r, tab \t)
        text = text.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");

        // 6. Normalize Dashes
        // Convert Em-dash (—) sequences like "word—word" to "word - word"
        text = text.replace("—", " - ");
        // Convert En-dash (–) to standard hyphen "-"
        text = text.replace("–", "-");

        // 7. Word Rejoining (Aggressive De-hyphenation)
        // Merges words split by line breaks (e.g., "ca-lculator" -> "calculator").
        // NOTE: This also affects compound words (e.g., "Post-its" -> "Postits"),
        // prioritizing the repair of broken text from PDFs.
        text = text.replaceAll("(?<=[a-zA-Z])-(?=[a-zA-Z])", "");

        // 4. Normalize whitespace
        // Replace multiple spaces with single space
        text = text.replaceAll("[ \\t]+", " ");
        // Replace multiple newlines with double newline (for paragraphs)
        text = text.replaceAll("(\\r?\\n){3,}", "\n\n");

        return text.trim();
    }
}
