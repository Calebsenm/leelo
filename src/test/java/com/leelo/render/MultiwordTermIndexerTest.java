package com.leelo.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the index of the multi-word terms.
 */
class MultiwordTermIndexerTest {

    @Test
    void findsTheTermsOfTheSequence() {
        MultiwordTermIndexer indexer = new MultiwordTermIndexer();
        indexer.add("b\u200B \u200Bc");
        indexer.add("d\u200B \u200Be");

        List<MultiwordTermIndexer.Match> matches = indexer.searchAll(
                List.of("a", " ", "b", " ", "c", " ", "d", " ", "e"));

        assertEquals(2, matches.size());
        assertEquals(2, matches.get(0).startIndex());
        assertEquals(6, matches.get(1).startIndex());
    }

    @Test
    void ignoresTheSingleWordTerms() {
        MultiwordTermIndexer indexer = new MultiwordTermIndexer();
        indexer.add("cat");

        assertTrue(indexer.searchAll(List.of("a", "cat")).isEmpty());
    }

    @Test
    void findsTheOverlappingTerms() {
        MultiwordTermIndexer indexer = new MultiwordTermIndexer();
        indexer.add("a\u200B \u200Bb");
        indexer.add("b\u200B \u200Bc");

        List<MultiwordTermIndexer.Match> matches = indexer.searchAll(List.of("a", " ", "b", " ", "c"));

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).startIndex());
        assertEquals(2, matches.get(1).startIndex());
    }
}
