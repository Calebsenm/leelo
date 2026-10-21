package com.leelo.render;

import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.parse.ParsedToken;
import com.leelo.parse.ParserRegistry;
import com.leelo.parse.TextParser;
import com.leelo.util.TextTokens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Turns a parsed page into the items that are actually rendered.
 *
 * <p>When terms overlap, the longest one wins. The tokens of the shorter term
 * that end up hidden are simply not displayed, which is why an item knows both
 * how many tokens it covers and how many of them remain visible.
 */
public class TextRenderer {

    /**
     * Builds the text items of a page.
     *
     * @param tokens  the parsed tokens of the page
     * @param terms   the terms known for the language
     * @param language the language the page belongs to
     * @param indexer the index of the multi-word terms, may be {@code null}
     * @return the items to render and the terms that were not known yet
     */
    public RenderResult render(List<ParsedToken> tokens,
                               List<Term> terms,
                               Language language,
                               MultiwordTermIndexer indexer) {
        List<Term> newTerms = createMissingUnknownTerms(tokens, terms, language);
        List<Term> allTerms = new ArrayList<>(terms);
        allTerms.addAll(newTerms);

        Map<String, Term> textToTerm = new HashMap<>();
        for (Term term : allTerms) {
            textToTerm.put(term.getTextLc(), term);
        }

        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        List<String> tokensOrig = new ArrayList<>(tokens.size());
        List<String> tokensLc = new ArrayList<>(tokens.size());
        for (ParsedToken token : tokens) {
            tokensOrig.add(token.getToken());
            tokensLc.add(parser.getLowercase(token.getToken(), language));
        }

        List<TextItem> items = new ArrayList<>();

        // One item per token, which also covers every single-word term.
        for (int index = 0; index < tokens.size(); index++) {
            addTextItem(items, tokensOrig, tokensLc, tokens, index, 1, textToTerm);
        }

        // The multi-word terms found in the page.
        MultiwordTermIndexer termIndexer = indexer != null ? indexer : buildIndexer(allTerms);
        for (MultiwordTermIndexer.Match match : termIndexer.searchAll(tokensLc)) {
            Term term = textToTerm.get(match.termTextLc());
            if (term == null) {
                continue;
            }
            addTextItem(items, tokensOrig, tokensLc, tokens, match.startIndex(),
                    term.getTokenCount(), textToTerm);
        }

        // Longest match first, so that it takes precedence over the shorter ones.
        items.sort(Comparator.comparingInt(TextItem::getIndex)
                .thenComparingInt(item -> -item.getTokenCount()));

        // "Write out" the items: the last writer for a token wins.
        TextItem[] winners = new TextItem[tokens.size()];
        for (int i = items.size() - 1; i >= 0; i--) {
            TextItem item = items.get(i);
            int end = Math.min(item.getIndex() + item.getTokenCount(), tokens.size());
            for (int position = item.getIndex(); position < end; position++) {
                winners[position] = item;
            }
        }

        // An item displays as many tokens as it actually won.
        Map<TextItem, Integer> displayCounts = new IdentityHashMap<>();
        for (TextItem winner : winners) {
            if (winner != null) {
                displayCounts.merge(winner, 1, Integer::sum);
            }
        }
        for (TextItem item : items) {
            item.setDisplayCount(displayCounts.getOrDefault(item, 0));
        }
        items.removeIf(item -> item.getDisplayCount() <= 0);

        int currentParagraph = 0;
        for (TextItem item : items) {
            item.setParagraphNumber(currentParagraph);
            if (ParsedToken.PARAGRAPH_MARKER.equals(item.getText())) {
                currentParagraph++;
            }
        }

        return new RenderResult(items, newTerms);
    }

    /**
     * Splits the rendered items into paragraphs and sentences.
     */
    public List<RenderedParagraph> buildParagraphs(List<TextItem> items) {
        List<RenderedParagraph> paragraphs = new ArrayList<>();
        List<TextItem> current = new ArrayList<>();
        for (TextItem item : items) {
            if (ParsedToken.PARAGRAPH_MARKER.equals(item.getText())) {
                addParagraph(paragraphs, current);
                current.clear();
            } else {
                current.add(item);
            }
        }
        addParagraph(paragraphs, current);
        return paragraphs;
    }

    /** Adds a paragraph, skipping the ones that hold nothing. */
    private void addParagraph(List<RenderedParagraph> paragraphs, List<TextItem> items) {
        if (items.isEmpty()) {
            return;
        }
        paragraphs.add(buildParagraph(paragraphs.size(), items));
    }

    private RenderedParagraph buildParagraph(int number, List<TextItem> items) {
        RenderedParagraph paragraph = new RenderedParagraph(number);
        RenderedSentence sentence = null;
        int lastSentenceNumber = -1;
        for (TextItem item : items) {
            if (sentence == null || item.getSentenceNumber() != lastSentenceNumber) {
                sentence = new RenderedSentence(item.getSentenceNumber());
                paragraph.getSentences().add(sentence);
                lastSentenceNumber = item.getSentenceNumber();
                item.addCssClass("sentencestart");
            }
            sentence.getItems().add(item);
        }
        return paragraph;
    }

    /** Adds an item covering the given number of tokens, starting at the index. */
    private void addTextItem(List<TextItem> items,
                             List<String> tokensOrig,
                             List<String> tokensLc,
                             List<ParsedToken> tokens,
                             int index,
                             int count,
                             Map<String, Term> textToTerm) {
        int end = Math.min(index + count, tokensOrig.size());
        String text = count > 1
                ? TextTokens.join(tokensOrig.subList(index, end))
                : tokensOrig.get(index);
        String textLc = count > 1
                ? TextTokens.join(tokensLc.subList(index, end))
                : tokensLc.get(index);
        Term term = textToTerm.get(textLc);

        TextItem item = new TextItem();
        item.setText(text);
        item.setTextLc(textLc);
        item.setTokenCount(count);
        item.setDisplayCount(count);
        item.setIndex(index);
        item.setWord(term != null);
        item.setSentenceNumber(tokens.get(index).getSentenceNumber());
        item.setTerm(term);
        items.add(item);
    }

    /**
     * Builds an index holding the multi-word terms of the given collection.
     */
    private MultiwordTermIndexer buildIndexer(List<Term> terms) {
        MultiwordTermIndexer termIndexer = new MultiwordTermIndexer();
        for (Term term : terms) {
            if (term.getTokenCount() > 1) {
                termIndexer.add(term.getTextLc());
            }
        }
        return termIndexer;
    }

    /**
     * Creates a term for every word of the page that is not known yet.
     *
     * <p>The new terms are not persisted here: the caller decides when they
     * should become part of the vocabulary.
     */
    private List<Term> createMissingUnknownTerms(List<ParsedToken> tokens,
                                                  List<Term> terms,
                                                  Language language) {
        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        Map<String, String> lowerToOriginal = new LinkedHashMap<>();
        for (ParsedToken token : tokens) {
            if (token.isWord()) {
                lowerToOriginal.put(parser.getLowercase(token.getToken(), language), token.getToken());
            }
        }

        Set<String> known = new HashSet<>();
        for (Term term : terms) {
            known.add(term.getTextLc());
        }

        List<Term> newTerms = new ArrayList<>();
        for (Map.Entry<String, String> entry : lowerToOriginal.entrySet()) {
            if (known.contains(entry.getKey())) {
                continue;
            }
            Term term = new Term();
            term.setLanguageId(language.getId());
            term.setLanguageName(language.getName());
            term.setText(entry.getValue());
            term.setTextLc(entry.getKey());
            term.setTokenCount(1);
            term.setStatus(TermStatus.UNKNOWN);
            newTerms.add(term);
        }
        return newTerms;
    }

    /** The outcome of a rendering: the items to display and the unknown terms. */
    public record RenderResult(List<TextItem> items, List<Term> newTerms) {
    }
}
