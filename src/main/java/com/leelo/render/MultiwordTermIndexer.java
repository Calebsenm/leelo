package com.leelo.render;

import com.leelo.util.TextTokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Index of the multi-word terms of a language.
 *
 * <p>The index is a trie over the token sequences of the terms, which makes
 * looking up every multi-word term of a page a single pass over its tokens.
 */
public class MultiwordTermIndexer {

    private final Node root = new Node();

    /**
     * Adds a term to the index.
     *
     * @param textLc the lower-cased, zero-width-space joined text of the term
     */
    public void add(String textLc) {
        String[] tokens = TextTokens.split(textLc);
        if (tokens.length < 2) {
            return;
        }
        Node node = root;
        for (String token : tokens) {
            node = node.children.computeIfAbsent(token, key -> new Node());
        }
        node.patterns.add(textLc);
    }

    /** Adds every given term to the index. */
    public void addAll(Iterable<String> termTexts) {
        for (String termText : termTexts) {
            add(termText);
        }
    }

    /**
     * Finds every multi-word term contained in the given token sequence.
     *
     * @param lowercasedTokens the lower-cased tokens of the page
     * @return one entry per occurrence, holding the term text and the index of
     *         the token the occurrence starts at
     */
    public List<Match> searchAll(List<String> lowercasedTokens) {
        List<Match> results = new ArrayList<>();
        for (int start = 0; start < lowercasedTokens.size(); start++) {
            Node node = root;
            for (int index = start; index < lowercasedTokens.size(); index++) {
                node = node.children.get(lowercasedTokens.get(index));
                if (node == null) {
                    break;
                }
                for (String pattern : node.patterns) {
                    results.add(new Match(pattern, start));
                }
            }
        }
        return results;
    }

    /** A term occurrence found in a token sequence. */
    public record Match(String termTextLc, int startIndex) {
    }

    /** A node of the trie. */
    private static final class Node {
        private final Map<String, Node> children = new HashMap<>();
        private final List<String> patterns = new ArrayList<>();
    }
}
