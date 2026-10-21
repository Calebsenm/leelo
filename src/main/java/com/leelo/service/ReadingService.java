package com.leelo.service;

import com.leelo.dao.BookStatsDAO;
import com.leelo.dao.ReadingEventDAO;
import com.leelo.model.Book;
import com.leelo.model.BookPage;
import com.leelo.model.Language;
import com.leelo.model.ReadingEvent;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.parse.ParsedToken;
import com.leelo.parse.ParserRegistry;
import com.leelo.render.MultiwordTermIndexer;
import com.leelo.render.RenderedParagraph;
import com.leelo.render.TextItem;
import com.leelo.render.TextRenderer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The reading workflow: rendering the pages, keeping track of what was read
 * and updating the status of the terms.
 */
public class ReadingService {

    private final TermService termService = new TermService();
    private final BookService bookService = new BookService();
    private final ReadingEventDAO readingEventDAO = new ReadingEventDAO();
    private final BookStatsDAO bookStatsDAO = new BookStatsDAO();
    private final TextRenderer textRenderer = new TextRenderer();

    /**
     * Renders a page of a book.
     *
     * @param book      the book the page belongs to
     * @param page      the page to render
     * @param language  the language of the book
     * @param trackOpen when true, the page is marked as opened and the words it
     *                  contains are added to the vocabulary
     */
    public PageContent getPageContent(Book book, BookPage page, Language language, boolean trackOpen) {
        if (trackOpen) {
            bookService.setPageStarted(page.getId());
            bookService.setCurrentPage(book.getId(), page.getId());
            bookStatsDAO.delete(book.getId());
        }

        List<ParsedToken> tokens = ParserRegistry.getParserOrDefault(language.getParserType())
                .getParsedTokens(page.getContent(), language);

        MultiwordTermIndexer indexer = termService.buildMultiwordIndexer(language.getId());
        List<Term> terms = termService.findTermsInTokens(tokens, language, indexer);

        TextRenderer.RenderResult result = textRenderer.render(tokens, terms, language, indexer);
        List<RenderedParagraph> paragraphs = textRenderer.buildParagraphs(result.items());

        if (trackOpen) {
            saveNewTerms(result.newTerms());
        }
        return new PageContent(page, paragraphs, result.items(), result.newTerms());
    }

    /** Adds the words of the page that were not known yet to the vocabulary. */
    private void saveNewTerms(List<Term> newTerms) {
        for (Term term : newTerms) {
            if (term.getId() == 0) {
                termService.save(term);
            }
        }
    }

    /**
     * Marks a page as read and records the reading event.
     */
    public void markPageRead(Book book, BookPage page, Language language) {
        bookService.setPageRead(page.getId(), true);
        ReadingEvent event = new ReadingEvent();
        event.setLanguageId(language.getId());
        event.setPageId(page.getId());
        event.setReadDate(Instant.now());
        event.setWordCount(page.getWordCount());
        readingEventDAO.insert(event);
        bookStatsDAO.delete(book.getId());
    }

    /**
     * Sets every word of the page that is still unknown to "Well Known".
     *
     * @return the number of terms that were updated
     */
    public int setUnknownsToKnown(BookPage page, Language language) {
        List<ParsedToken> tokens = ParserRegistry.getParserOrDefault(language.getParserType())
                .getParsedTokens(page.getContent(), language);
        MultiwordTermIndexer indexer = termService.buildMultiwordIndexer(language.getId());
        List<Term> terms = termService.findTermsInTokens(tokens, language, indexer);

        Set<Integer> unknown = new LinkedHashSet<>();
        for (Term term : terms) {
            if (term.getStatus() == TermStatus.UNKNOWN) {
                unknown.add(term.getId());
            }
        }
        termService.setStatusForAll(new ArrayList<>(unknown), TermStatus.WELL_KNOWN);
        return unknown.size();
    }

    /**
     * Changes the status of several terms at once.
     *
     * @return the number of terms that were updated
     */
    public int bulkStatusUpdate(List<Integer> termIds, TermStatus status) {
        if (termIds == null || termIds.isEmpty()) {
            return 0;
        }
        termService.setStatusForAll(termIds, status);
        return termIds.size();
    }

    /** The rendered content of a page. */
    public record PageContent(BookPage page,
                              List<RenderedParagraph> paragraphs,
                              List<TextItem> items,
                              List<Term> newTerms) {

        /** The plain text of the page, without the hidden parts of the terms. */
        public String getPlainText() {
            StringBuilder builder = new StringBuilder();
            for (RenderedParagraph paragraph : paragraphs) {
                builder.append(paragraph.getPlainText()).append('\n');
            }
            return builder.toString().trim();
        }
    }
}
