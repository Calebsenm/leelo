package com.leelo.service;

import com.leelo.model.Language;
import com.leelo.parse.ParsedToken;
import com.leelo.parse.ParserRegistry;
import com.leelo.parse.TextParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a text into the pages of a book.
 *
 * <p>The text is first cut at the explicit page breaks, then the remaining
 * parts are cut again once they hold more words than the configured threshold,
 * so that the pages stay a comfortable size to read.
 */
public final class PageSplitter {

    /** How the pages are cut. */
    public enum SplitBy {
        PARAGRAPHS("paragraphs"),
        SENTENCES("sentences");

        private final String code;

        SplitBy(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static SplitBy fromCode(String code) {
            for (SplitBy value : values()) {
                if (value.code.equalsIgnoreCase(code)) {
                    return value;
                }
            }
            return PARAGRAPHS;
        }
    }

    /** Default number of words a page may hold. */
    public static final int DEFAULT_THRESHOLD = 250;

    private PageSplitter() {
    }

    /**
     * Splits the text into pages.
     *
     * @param text            the raw text
     * @param language        the language the text is written in
     * @param splitBy         whether the pages break on paragraphs or sentences
     * @param thresholdTokens number of words a page may hold
     */
    public static List<String> split(String text, Language language, SplitBy splitBy, int thresholdTokens) {
        List<String> pages = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return pages;
        }
        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        for (String segment : splitAtPageBreaks(text)) {
            List<ParsedToken> tokens = parser.getParsedTokens(segment, language);
            pages.addAll(groupTokens(tokens, splitBy, thresholdTokens));
        }
        return pages;
    }

    /**
     * Cuts the text at the lines made of three dashes, which is the explicit
     * page break of the imported texts.
     */
    public static List<String> splitAtPageBreaks(String text) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\r?\n")) {
            if (line.strip().equals("---")) {
                segments.add(current.toString().strip());
                current.setLength(0);
            } else {
                current.append(line).append('\n');
            }
        }
        if (!current.isEmpty()) {
            segments.add(current.toString().strip());
        }
        return segments;
    }

    /**
     * Groups the tokens into pages.
     *
     * <p>The tokens are gathered sentence by sentence, or paragraph by
     * paragraph, and a page is closed as soon as it holds more words than the
     * threshold. The paragraph markers are trimmed from the edges of the pages.
     */
    private static List<String> groupTokens(List<ParsedToken> tokens, SplitBy splitBy, int threshold) {
        List<String> pages = new ArrayList<>();
        List<ParsedToken> currentGroup = new ArrayList<>();
        List<ParsedToken> buffer = new ArrayList<>();

        for (ParsedToken token : tokens) {
            buffer.add(token);
            if (!matchesDelimiter(token, splitBy)) {
                continue;
            }
            currentGroup.addAll(buffer);
            buffer.clear();
            if (countWords(currentGroup) > threshold) {
                pages.add(toPage(trimParagraphMarkers(currentGroup)));
                currentGroup.clear();
            }
        }

        if (!buffer.isEmpty()) {
            currentGroup.addAll(buffer);
        }
        String page = toPage(trimParagraphMarkers(currentGroup));
        if (!page.isEmpty()) {
            pages.add(page);
        }
        return pages;
    }

    /** True when the token closes a group of the given kind. */
    private static boolean matchesDelimiter(ParsedToken token, SplitBy splitBy) {
        return splitBy == SplitBy.SENTENCES
                ? token.isEndOfSentence()
                : token.isEndOfParagraph();
    }

    /** Counts the words of the tokens. */
    private static int countWords(List<ParsedToken> tokens) {
        int count = 0;
        for (ParsedToken token : tokens) {
            if (token.isWord()) {
                count++;
            }
        }
        return count;
    }

    /** Removes the paragraph markers from the edges of a group. */
    private static List<ParsedToken> trimParagraphMarkers(List<ParsedToken> tokens) {
        List<ParsedToken> trimmed = new ArrayList<>(tokens);
        while (!trimmed.isEmpty() && trimmed.get(0).isEndOfParagraph()) {
            trimmed.remove(0);
        }
        while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).isEndOfParagraph()) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }

    /** Builds the text of a page out of its tokens. */
    private static String toPage(List<ParsedToken> tokens) {
        StringBuilder builder = new StringBuilder();
        for (ParsedToken token : tokens) {
            builder.append(token.getToken());
        }
        return builder.toString()
                .replace("\r", "")
                .replace(ParsedToken.PARAGRAPH_MARKER, "\n")
                .strip();
    }
}
