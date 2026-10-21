package com.leelo.service;

import com.leelo.dao.TermDAO;
import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.model.TermTag;
import com.leelo.parse.ParsedToken;
import com.leelo.parse.ParserRegistry;
import com.leelo.parse.TextParser;
import com.leelo.render.MultiwordTermIndexer;
import com.leelo.util.TextTokens;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Manages the vocabulary: the terms, their status, their tags and their parents. */
public class TermService {

    private final TermDAO termDAO = new TermDAO();
    private final TermTagService termTagService = new TermTagService();

    /** Returns every term of a language, or of every language when the id is zero. */
    public List<Term> getAll(int languageId) {
        return termDAO.findAll(languageId);
    }

    /** Returns the term with the given id, or {@code null}. */
    public Term getById(int id) {
        return termDAO.findById(id);
    }

    /** Returns the term of a language with the given lower-cased text, or {@code null}. */
    public Term getByTextLc(int languageId, String textLc) {
        return termDAO.findByTextLc(languageId, textLc);
    }

    /** Returns the terms whose text contains the given query. */
    public List<Term> search(int languageId, String query, int limit) {
        return termDAO.search(languageId, query.trim().toLowerCase(), limit);
    }

    /**
     * Builds the term out of its text.
     *
     * <p>The text is parsed with the rules of the language, so that the word
     * boundaries are kept and a multi-word term can be recognised later on.
     */
    public Term buildTerm(Language language, String text) {
        Term term = new Term();
        term.setLanguageId(language.getId());
        term.setLanguageName(language.getName());
        term.setStatus(TermStatus.NEW_1);

        String parsed = parseText(language, text);
        term.setText(parsed);
        term.setTextLc(lowercase(language, parsed));
        term.setTokenCount(TextTokens.countTokens(parsed));
        term.setRomanization(ParserRegistry.getParserOrDefault(language.getParserType())
                .getReading(text, language));
        return term;
    }

    /** Parses a text into its zero-width-space joined tokens. */
    public String parseText(Language language, String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.strip()
                .replace(TextTokens.ZWS, "")
                .replace("\u00A0", " ");
        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        List<String> tokens = new ArrayList<>();
        for (ParsedToken token : parser.getParsedTokens(cleaned, language)) {
            if (!token.isEndOfParagraph()) {
                tokens.add(token.getToken());
            }
        }
        return TextTokens.join(tokens);
    }

    /** Lower-cases a text following the rules of the language. */
    public String lowercase(Language language, String text) {
        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        List<String> tokens = new ArrayList<>();
        for (String token : TextTokens.split(text)) {
            tokens.add(parser.getLowercase(token, language));
        }
        return TextTokens.join(tokens);
    }

    /**
     * Returns the term of the language with the given text, creating it when it
     * does not exist yet.
     */
    public Term findOrCreate(Language language, String text) {
        String textLc = lowercase(language, parseText(language, text));
        Term existing = termDAO.findByTextLc(language.getId(), textLc);
        if (existing != null) {
            return existing;
        }
        Term term = buildTerm(language, text);
        term.setStatus(TermStatus.UNKNOWN);
        termDAO.insert(term);
        return term;
    }

    /**
     * Creates or updates a term.
     *
     * @return the id of the term, or zero when the term is not valid
     */
    public int save(Term term) {
        String error = validate(term);
        if (error != null) {
            return 0;
        }
        if (term.getId() > 0) {
            termDAO.update(term);
            propagateStatus(term);
            return term.getId();
        }
        int id = termDAO.insert(term);
        propagateStatus(term);
        return id;
    }

    /**
     * Validates a term.
     *
     * @return the reason why the term is not valid, or {@code null}
     */
    public String validate(Term term) {
        if (term.getText() == null || term.getText().isBlank()) {
            return "The term is required.";
        }
        if (term.getLanguageId() <= 0) {
            return "Please select a language.";
        }
        Term existing = termDAO.findByTextLc(term.getLanguageId(), term.getTextLc());
        if (existing != null && existing.getId() != term.getId()) {
            return "Term already exists.";
        }
        return null;
    }

    /** Changes the status of a term and propagates it to the linked terms. */
    public boolean setStatus(int termId, TermStatus status) {
        Term term = termDAO.findById(termId);
        if (term == null) {
            return false;
        }
        termDAO.setStatus(termId, status);
        propagateStatus(term);
        return true;
    }

    /** Changes the status of several terms at once. */
    public void setStatusForAll(List<Integer> termIds, TermStatus status) {
        for (Integer termId : termIds) {
            setStatus(termId, status);
        }
    }

    /** Deletes a term. */
    public boolean delete(int termId) {
        return termDAO.delete(termId);
    }

    /** Stores the image of a term. */
    public boolean setImage(int termId, String source) {
        return termDAO.saveImage(termId, source);
    }

    /** Stores the flash message of a term. */
    public boolean setFlashMessage(int termId, String message) {
        return termDAO.saveFlashMessage(termId, message);
    }

    /** Returns the terms of a language whose text is one of the given ones. */
    public List<Term> findTermsInTokens(List<ParsedToken> tokens,
                                         Language language,
                                         MultiwordTermIndexer indexer) {
        TextParser parser = ParserRegistry.getParserOrDefault(language.getParserType());
        Set<String> candidates = new LinkedHashSet<>();
        for (ParsedToken token : tokens) {
            candidates.add(parser.getLowercase(token.getToken(), language));
        }
        if (indexer != null) {
            List<String> lowercased = new ArrayList<>();
            for (ParsedToken token : tokens) {
                lowercased.add(parser.getLowercase(token.getToken(), language));
            }
            for (MultiwordTermIndexer.Match match : indexer.searchAll(lowercased)) {
                candidates.add(match.termTextLc());
            }
        }
        return termDAO.findByTextLcs(language.getId(), candidates);
    }

    /** Builds the index of the multi-word terms of a language. */
    public MultiwordTermIndexer buildMultiwordIndexer(int languageId) {
        MultiwordTermIndexer indexer = new MultiwordTermIndexer();
        indexer.addAll(termDAO.findMultiWordTexts(languageId));
        return indexer;
    }

    /** Turns the tag names into the tags themselves, creating the missing ones. */
    public List<TermTag> resolveTags(List<String> tagTexts) {
        List<TermTag> tags = new ArrayList<>();
        if (tagTexts == null) {
            return tags;
        }
        for (String tagText : tagTexts) {
            if (tagText == null || tagText.isBlank()) {
                continue;
            }
            TermTag tag = termTagService.findOrCreateByText(tagText.trim());
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * Propagates a status change to the terms that are linked to it.
     *
     * <p>A term that follows its parent shares its status with it; a parent
     * with a single child that follows it shares the status back.
     */
    private void propagateStatus(Term term) {
        if (!term.isSyncStatus() || term.getParents().size() != 1) {
            return;
        }
        Term parent = term.getParents().get(0);
        if (parent.getId() <= 0) {
            return;
        }
        if (parent.getStatus() != term.getStatus()) {
            termDAO.setStatus(parent.getId(), term.getStatus());
        }
        for (Term child : termDAO.findAll(term.getLanguageId())) {
            if (child.getId() == term.getId() || !child.isSyncStatus()) {
                continue;
            }
            if (child.getParents().size() == 1 && child.getParents().get(0).getId() == parent.getId()) {
                if (child.getStatus() != parent.getStatus()) {
                    termDAO.setStatus(child.getId(), parent.getStatus());
                }
            }
        }
    }
}
