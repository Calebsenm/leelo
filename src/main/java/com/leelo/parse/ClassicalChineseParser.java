package com.leelo.parse;

import com.leelo.model.Language;
import com.leelo.util.CharClass;
import com.leelo.util.TextTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for character-based languages such as Classical Chinese, where every
 * character is a token of its own.
 */
public class ClassicalChineseParser implements TextParser {

    public static final String TYPE = "classicalchinese";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "Classical Chinese";
    }

    @Override
    public List<ParsedToken> getParsedTokens(String text, Language language) {
        List<ParsedToken> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        // Character-based languages do not use spaces between words.
        String prepared = text.replaceAll("[ \t]+", "")
                .replace(TextTokens.ZWS, "")
                .replace("\r\n", "\n")
                .replace("\n", ParsedToken.PARAGRAPH_MARKER);

        CharClass wordCharacters = CharClass.parse(language.getWordCharacters() == null
                || language.getWordCharacters().trim().isEmpty()
                ? "一-鿿"
                : language.getWordCharacters().trim());
        CharClass splitCharacters = CharClass.parse(language.getRegexpSplitSentences() == null
                || language.getRegexpSplitSentences().trim().isEmpty()
                ? ".!?"
                : language.getRegexpSplitSentences().trim());

        int order = 0;
        int sentenceNumber = 0;
        for (int i = 0; i < prepared.length(); ) {
            int codePoint = prepared.codePointAt(i);
            String character = new String(Character.toChars(codePoint));
            i += Character.charCount(codePoint);

            boolean isWord = wordCharacters.matches(character.charAt(0));
            boolean endOfSentence = splitCharacters.matches(character.charAt(0))
                    || ParsedToken.PARAGRAPH_MARKER.equals(character);

            tokens.add(new ParsedToken(character, isWord, endOfSentence, order++, sentenceNumber));
            if (endOfSentence) {
                sentenceNumber++;
            }
        }
        return tokens;
    }
}
