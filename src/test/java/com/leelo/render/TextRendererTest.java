package com.leelo.render;

import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.parse.ParsedToken;
import com.leelo.parse.SpaceDelimitedParser;
import com.leelo.parse.TextParser;
import com.leelo.util.TextTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the rendering of a page, in particular the way overlapping terms
 * are resolved.
 */
class TextRendererTest {

    private Language language;
    private TextParser parser;
    private TextRenderer renderer;

    @BeforeEach
    void setUp() {
        language = new Language();
        language.setId(1);
        language.setName("English");
        language.setParserType(SpaceDelimitedParser.TYPE);
        parser = new SpaceDelimitedParser();
        renderer = new TextRenderer();
    }

    @Test
    void displaysEveryWordOfThePage() {
        List<Term> terms = List.of(term("a"), term("b"), term("c"), term("d"), term("e"));

        List<TextItem> items = render("a b c d e", terms);

        assertEquals("a b c d e", plainText(items));
    }

    @Test
    void theLongestTermWins() {
        Term longTerm = multiWordTerm("b c");
        Term shortTerm = term("b");

        List<TextItem> items = render("a b c", List.of(longTerm, shortTerm));

        // The short term is completely covered by the long one.
        assertFalse(items.stream().anyMatch(item -> item.getTerm() == shortTerm));
        assertTrue(items.stream().anyMatch(item -> item.getTerm() == longTerm));
        assertEquals("a b c", plainText(items));
    }

    @Test
    void hidesThePartOfATermThatIsCovered() {
        Term first = multiWordTerm("b c d");
        Term second = multiWordTerm("d e");

        List<TextItem> items = render("a b c d e", List.of(first, second));

        TextItem overlapping = items.stream()
                .filter(item -> item.getTerm() == second)
                .findFirst()
                .orElseThrow();

        assertTrue(overlapping.isOverlapped());
        assertEquals(2, overlapping.getDisplayCount());
        assertEquals(3, overlapping.getTokenCount());
        assertEquals("a b c d e", plainText(items));
    }

    @Test
    void createsTheUnknownWordsOfThePage() {
        TextRenderer.RenderResult result = renderer.render(tokens("a b c"), List.of(term("a")), language, null);

        assertEquals(2, result.newTerms().size());
        assertTrue(result.newTerms().stream().allMatch(item -> item.getStatus() == TermStatus.UNKNOWN));
    }

    @Test
    void groupsTheItemsInParagraphsAndSentences() {
        List<RenderedParagraph> paragraphs = renderer.buildParagraphs(render("One. Two\n\nThree", List.of()));

        assertEquals(2, paragraphs.size());
        assertEquals(2, paragraphs.get(0).getSentences().size());
        assertEquals(1, paragraphs.get(1).getSentences().size());
    }

    @Test
    void givesEveryItemItsPositionInThePage() {
        List<TextItem> items = render("a b c", List.of());

        assertEquals(0, items.get(0).getIndex());
        assertEquals(1, items.get(1).getIndex());
        assertEquals(2, items.get(2).getIndex());
    }

    private List<TextItem> render(String text, List<Term> terms) {
        return renderer.render(tokens(text), terms, language, null).items();
    }

    private List<ParsedToken> tokens(String text) {
        return parser.getParsedTokens(text, language);
    }

    /** Builds a term made of a single word. */
    private Term term(String text) {
        Term term = new Term();
        term.setLanguageId(language.getId());
        term.setText(text);
        term.setTextLc(text.toLowerCase(Locale.ROOT));
        term.setTokenCount(1);
        term.setStatus(TermStatus.NEW_1);
        return term;
    }

    /**
     * Builds a term out of several words, keeping the token boundaries the way
     * the application does.
     */
    private Term multiWordTerm(String text) {
        List<String> tokens = new ArrayList<>();
        for (ParsedToken token : parser.getParsedTokens(text, language)) {
            tokens.add(token.getToken());
        }
        List<String> lowercased = new ArrayList<>();
        for (ParsedToken token : parser.getParsedTokens(text, language)) {
            lowercased.add(parser.getLowercase(token.getToken(), language));
        }
        Term term = new Term();
        term.setLanguageId(language.getId());
        term.setText(TextTokens.join(tokens));
        term.setTextLc(TextTokens.join(lowercased));
        term.setTokenCount(tokens.size());
        term.setStatus(TermStatus.NEW_1);
        return term;
    }

    /** Returns the visible text of the items. */
    private String plainText(List<TextItem> items) {
        StringBuilder builder = new StringBuilder();
        for (TextItem item : items) {
            builder.append(item.getPlainDisplayText());
        }
        return builder.toString();
    }
}
