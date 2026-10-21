package com.leelo.parse;

import com.leelo.model.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the space delimited parser.
 */
class SpaceDelimitedParserTest {

    private Language language;
    private SpaceDelimitedParser parser;

    @BeforeEach
    void setUp() {
        language = new Language();
        language.setName("English");
        language.setParserType(SpaceDelimitedParser.TYPE);
        parser = new SpaceDelimitedParser();
    }

    @Test
    void splitsWordsAndPunctuation() {
        List<ParsedToken> tokens = parser.getParsedTokens("Hello, world.", language);

        assertEquals(4, tokens.size());
        assertEquals("Hello", tokens.get(0).getToken());
        assertTrue(tokens.get(0).isWord());
        assertEquals(", ", tokens.get(1).getToken());
        assertFalse(tokens.get(1).isWord());
        assertEquals("world", tokens.get(2).getToken());
        assertTrue(tokens.get(2).isWord());
        assertEquals(".", tokens.get(3).getToken());
        assertFalse(tokens.get(3).isWord());
    }

    @Test
    void marksTheEndOfASentence() {
        List<ParsedToken> tokens = parser.getParsedTokens("One. Two", language);

        assertEquals(3, tokens.size());
        assertTrue(tokens.get(1).isEndOfSentence());
        assertFalse(tokens.get(2).isEndOfSentence());
    }

    @Test
    void keepsAbbreviationsTogether() {
        List<ParsedToken> tokens = parser.getParsedTokens("Dr. Smith", language);

        assertEquals("Dr.", tokens.get(0).getToken());
        assertTrue(tokens.get(0).isWord());
    }

    @Test
    void separatesTheParagraphs() {
        List<ParsedToken> tokens = parser.getParsedTokens("First\n\nSecond", language);

        boolean foundMarker = tokens.stream().anyMatch(token -> token.isEndOfParagraph());
        assertTrue(foundMarker);
    }

    @Test
    void appliesTheCharacterSubstitutions() {
        language.setCharacterSubstitutions("´='|'='");

        List<ParsedToken> tokens = parser.getParsedTokens("l´eau", language);

        StringBuilder joined = new StringBuilder();
        for (ParsedToken token : tokens) {
            joined.append(token.getToken());
        }
        assertEquals("l'eau", joined.toString());
    }

    @Test
    void numbersTheSentences() {
        List<ParsedToken> tokens = parser.getParsedTokens("One. Two. Three", language);

        assertEquals(0, tokens.get(0).getSentenceNumber());
        assertEquals(0, tokens.get(1).getSentenceNumber());
        assertEquals(1, tokens.get(2).getSentenceNumber());
        assertEquals(2, tokens.get(4).getSentenceNumber());
    }

    @Test
    void lowerCasesUsingTheRootLocale() {
        assertEquals("straße", parser.getLowercase("STRAßE", language));
    }
}
