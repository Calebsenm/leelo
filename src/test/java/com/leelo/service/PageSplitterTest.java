package com.leelo.service;

import com.leelo.model.Language;
import com.leelo.parse.SpaceDelimitedParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the way the texts are split into pages.
 */
class PageSplitterTest {

    private Language language;

    @BeforeEach
    void setUp() {
        language = new Language();
        language.setName("English");
        language.setParserType(SpaceDelimitedParser.TYPE);
    }

    @Test
    void cutsTheTextAtTheExplicitPageBreaks() {
        List<String> pages = PageSplitter.split("First page\n---\nSecond page", language,
                PageSplitter.SplitBy.PARAGRAPHS, 250);

        assertEquals(2, pages.size());
        assertEquals("First page", pages.get(0));
        assertEquals("Second page", pages.get(1));
    }

    @Test
    void cutsThePageWhenItHoldsTooManyWords() {
        List<String> pages = PageSplitter.split("One. Two. Three. Four. Five. Six.", language,
                PageSplitter.SplitBy.SENTENCES, 3);

        assertEquals(2, pages.size());
        assertEquals("One. Two. Three. Four.", pages.get(0));
        assertEquals("Five. Six.", pages.get(1));
    }

    @Test
    void keepsTheTextWithoutSentencesInOnePage() {
        List<String> pages = PageSplitter.split("one two three four five six seven eight", language,
                PageSplitter.SplitBy.SENTENCES, 3);

        assertEquals(1, pages.size());
    }

    @Test
    void keepsTheSentencesTogetherWhenSplittingBySentence() {
        List<String> pages = PageSplitter.split("One. Two. Three.", language,
                PageSplitter.SplitBy.SENTENCES, 250);

        assertEquals(1, pages.size());
        assertEquals("One. Two. Three.", pages.get(0));
    }

    @Test
    void ignoresEmptyTexts() {
        assertTrue(PageSplitter.split("   ", language, PageSplitter.SplitBy.PARAGRAPHS, 250).isEmpty());
    }
}
