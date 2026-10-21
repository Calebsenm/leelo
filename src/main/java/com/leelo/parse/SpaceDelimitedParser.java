package com.leelo.parse;

import com.leelo.model.Language;
import com.leelo.util.CharClass;
import com.leelo.util.TextTokens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for space-delimited languages such as English, French or Spanish.
 *
 * <p>The language settings drive the parsing: the word characters define what
 * counts as a word, the exceptions keep abbreviations such as "Mr." together and
 * the split characters mark the end of a sentence.
 */
public class SpaceDelimitedParser implements TextParser {

    public static final String TYPE = "spacedel";

    private static volatile String defaultWordCharacters;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "Space Delimited";
    }

    @Override
    public List<ParsedToken> getParsedTokens(String text, Language language) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        String cleaned = text.replaceAll(" +", " ").replace(TextTokens.ZWS, "");
        return parseToTokens(cleaned, language);
    }

    /**
     * Parses the already cleaned text into tokens.
     *
     * <p>Paragraphs are separated by a paragraph marker token so that the
     * renderer can rebuild the original layout.
     */
    protected List<ParsedToken> parseToTokens(String text, Language language) {
        String prepared = applyCharacterSubstitutions(text, language)
                .replace("\r\n", "\n")
                .replace("{", "[")
                .replace("}", "]");

        List<RawToken> rawTokens = new ArrayList<>();
        String[] paragraphs = prepared.split("\n");
        for (int i = 0; i < paragraphs.length; i++) {
            parseParagraph(paragraphs[i], language, rawTokens);
            if (i < paragraphs.length - 1) {
                rawTokens.add(new RawToken(ParsedToken.PARAGRAPH_MARKER, false, true));
            }
        }
        return assignNumbers(rawTokens);
    }

    /** Applies the "from=to|from=to" substitutions configured on the language. */
    private String applyCharacterSubstitutions(String text, Language language) {
        String substitutions = language.getCharacterSubstitutions();
        if (substitutions == null || substitutions.isBlank()) {
            return text;
        }
        String result = text;
        for (String replacement : substitutions.split("\\|")) {
            String[] parts = replacement.split("=");
            if (parts.length >= 2) {
                String from = parts[0].trim();
                String to = parts[1].trim();
                if (!from.isEmpty()) {
                    result = result.replace(from, to);
                }
            }
        }
        return result;
    }

    /**
     * Parses a single paragraph, appending the tokens it contains.
     *
     * <p>The scan is explicit rather than regex driven so that the result never
     * depends on how a regular expression engine handles empty matches: at
     * every position an exception is tried first, then a run of word
     * characters, and everything else is accumulated as plain text.
     */
    protected void parseParagraph(String text, Language language, List<RawToken> tokens) {
        if (text.isEmpty()) {
            return;
        }
        CharClass wordCharacters = getWordCharacters(language);
        CharClass splitCharacters = getSplitCharacters(language);
        Pattern exceptions = getExceptionPattern(language);

        StringBuilder plainText = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int exceptionLength = matchException(exceptions, text, index);
            if (exceptionLength > 0) {
                flushPlainText(plainText, splitCharacters, tokens);
                tokens.add(new RawToken(text.substring(index, index + exceptionLength), true, false));
                index += exceptionLength;
                continue;
            }

            char current = text.charAt(index);
            if (wordCharacters.matches(current)) {
                flushPlainText(plainText, splitCharacters, tokens);
                int end = index;
                while (end < text.length() && wordCharacters.matches(text.charAt(end))) {
                    end++;
                }
                tokens.add(new RawToken(text.substring(index, end), true, false));
                index = end;
                continue;
            }

            plainText.append(current);
            index++;
        }
        flushPlainText(plainText, splitCharacters, tokens);
    }

    /**
     * Adds the accumulated plain text as a single token, marking it as the end
     * of a sentence when it contains a sentence terminator.
     */
    private void flushPlainText(StringBuilder plainText, CharClass splitCharacters, List<RawToken> tokens) {
        if (plainText.isEmpty()) {
            return;
        }
        boolean endOfSentence = false;
        for (int i = 0; i < plainText.length(); i++) {
            if (splitCharacters.matches(plainText.charAt(i))) {
                endOfSentence = true;
                break;
            }
        }
        tokens.add(new RawToken(plainText.toString(), false, endOfSentence));
        plainText.setLength(0);
    }

    /** Returns the length of the exception matching at the given position, or zero. */
    private int matchException(Pattern exceptions, String text, int index) {
        if (exceptions == null) {
            return 0;
        }
        Matcher matcher = exceptions.matcher(text);
        matcher.region(index, text.length());
        matcher.useAnchoringBounds(false);
        matcher.useTransparentBounds(true);
        if (matcher.lookingAt()) {
            int end = matcher.end();
            return end > index ? end - index : 0;
        }
        return 0;
    }

    /** Returns the word characters configured on the language. */
    protected CharClass getWordCharacters(Language language) {
        String specification = language.getWordCharacters() == null ? "" : language.getWordCharacters().trim();
        if (specification.isEmpty()) {
            specification = getDefaultWordCharacters();
        }
        return CharClass.parse(specification);
    }

    /** Returns the sentence terminators configured on the language. */
    protected CharClass getSplitCharacters(Language language) {
        String specification = language.getRegexpSplitSentences() == null
                ? "" : language.getRegexpSplitSentences().trim();
        if (specification.isEmpty()) {
            specification = getDefaultSplitSentences();
        }
        return CharClass.parse(specification);
    }

    /** Returns the compiled pattern of the sentence-splitting exceptions. */
    protected Pattern getExceptionPattern(Language language) {
        String exceptions = language.getExceptionsSplitSentences() == null
                ? "" : language.getExceptionsSplitSentences();
        if (exceptions.trim().isEmpty()) {
            return null;
        }
        return Pattern.compile("(?:" + exceptions.replace(".", "\\.") + ")",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Assigns the order and sentence numbers to the parsed tokens. */
    private List<ParsedToken> assignNumbers(List<RawToken> rawTokens) {
        List<ParsedToken> tokens = new ArrayList<>(rawTokens.size());
        int order = 0;
        int sentenceNumber = 0;
        for (RawToken raw : rawTokens) {
            tokens.add(new ParsedToken(raw.text(), raw.word(), raw.endOfSentence(), order++, sentenceNumber));
            if (raw.endOfSentence()) {
                sentenceNumber++;
            }
        }
        return tokens;
    }

    /**
     * Returns the default set of word characters: every letter, every combining
     * mark and every connector or modifier symbol of the Unicode standard.
     */
    public static String getDefaultWordCharacters() {
        String cached = defaultWordCharacters;
        if (cached != null) {
            return cached;
        }
        StringBuilder ranges = new StringBuilder();
        int rangeStart = -1;
        int rangeEnd = -1;
        for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (isWordCharacter(codePoint)) {
                if (rangeStart < 0) {
                    rangeStart = codePoint;
                    rangeEnd = codePoint;
                } else if (codePoint == rangeEnd + 1) {
                    rangeEnd = codePoint;
                } else {
                    appendRange(ranges, rangeStart, rangeEnd);
                    rangeStart = codePoint;
                    rangeEnd = codePoint;
                }
            } else if (rangeStart >= 0) {
                appendRange(ranges, rangeStart, rangeEnd);
                rangeStart = -1;
            }
        }
        if (rangeStart >= 0) {
            appendRange(ranges, rangeStart, rangeEnd);
        }
        cached = ranges.toString();
        defaultWordCharacters = cached;
        return cached;
    }

    private static boolean isWordCharacter(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.FORMAT
                || type == Character.LOWERCASE_LETTER
                || type == Character.MODIFIER_LETTER
                || type == Character.OTHER_LETTER
                || type == Character.TITLECASE_LETTER
                || type == Character.UPPERCASE_LETTER
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.NON_SPACING_MARK
                || type == Character.MODIFIER_SYMBOL;
    }

    private static void appendRange(StringBuilder builder, int start, int end) {
        if (start == end) {
            builder.append(String.format("\\u%04x", start));
        } else {
            builder.append(String.format("\\u%04x", start))
                    .append('-')
                    .append(String.format("\\u%04x", end));
        }
    }

    /**
     * Returns the default set of sentence terminators, covering the Unicode
     * "ATerm" and "STerm" categories plus the colon.
     */
    public static String getDefaultSplitSentences() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join("",
                "\\.\\!\\?:",
                "\u2024\uFE52\uFF0E",
                "\u0589",
                "\u061D-\u061F\u06D4",
                "\u0700-\u0702",
                "\u07F9",
                "\u0837\u0839\u083D\u083E",
                "\u0964\u0965",
                "\u104A\u104B",
                "\u1362\u1367\u1368",
                "\u166E",
                "\u1735\u1736",
                "\u17D4\u17D5",
                "\u1803\u1809",
                "\u1944\u1945",
                "\u1AA8-\u1AAB",
                "\u1B5A\u1B5B\u1B5E\u1B5F\u1B7D\u1B7E",
                "\u1C3B\u1C3C",
                "\u1C7E\u1C7F",
                "\u203C\u203D\u2047-\u2049\u2E2E\u2E3C\u2E53\u2E54\u3002",
                "\uA4FF",
                "\uA60E\uA60F",
                "\uA6F3\uA6F7",
                "\uA876\uA877",
                "\uA8CE\uA8CF",
                "\uA92F",
                "\uA9C8\uA9C9",
                "\uAA5D\uAA5F",
                "\uAAF0\uAAF1\uABEB",
                "\uFE56\uFE57\uFF01\uFF1F\uFF61"));

        // The remaining terminators live outside the basic multilingual plane,
        // so they are given as code points rather than as escape sequences.
        for (int codePoint : SUPPLEMENTARY_SENTENCE_TERMINATORS) {
            builder.appendCodePoint(codePoint);
        }
        return builder.toString();
    }

    /**
     * Sentence terminators outside the basic multilingual plane, taken from the
     * Unicode "ATerm" and "STerm" categories.
     */
    private static final int[] SUPPLEMENTARY_SENTENCE_TERMINATORS = {
            0x10A56, 0x10A57,
            0x10F55, 0x10F56, 0x10F57, 0x10F58, 0x10F59,
            0x10F86, 0x10F87, 0x10F88, 0x10F89,
            0x11047, 0x11048,
            0x110BE, 0x110BF, 0x110C0, 0x110C1,
            0x11141, 0x11142, 0x11143,
            0x111C5, 0x111C6, 0x111CD, 0x111DE, 0x111DF,
            0x11238, 0x11239, 0x1123B, 0x1123C,
            0x112A9,
            0x1144B, 0x1144C,
            0x115C2, 0x115C3,
            0x115C9, 0x115CA, 0x115CB, 0x115CC, 0x115CD, 0x115CE, 0x115CF,
            0x115D0, 0x115D1, 0x115D2, 0x115D3, 0x115D4, 0x115D5, 0x115D6, 0x115D7,
            0x11641, 0x11642,
            0x1173C, 0x1173D, 0x1173E,
            0x11944, 0x11946,
            0x11A42, 0x11A43,
            0x11A9B, 0x11A9C,
            0x11C41, 0x11C42,
            0x11EF7, 0x11EF8,
            0x11F43, 0x11F44,
            0x16A6E, 0x16A6F,
            0x16AF5, 0x16B37, 0x16B38, 0x16B44,
            0x16E98,
            0x1BC9F,
            0x1DA88
    };

    /** Lower-cases the text, using the root locale so the result is deterministic. */
    @Override
    public String getLowercase(String text, Language language) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    /** Intermediate token, before the order and sentence numbers are assigned. */
    protected record RawToken(String text, boolean word, boolean endOfSentence) {
    }
}
