package com.leelo.service;

import com.leelo.dao.LanguageDAO;
import com.leelo.model.Language;
import com.leelo.model.LanguageDictionary;
import com.leelo.parse.ParserRegistry;

import java.util.ArrayList;
import java.util.List;

/** Manages the languages and their dictionaries. */
public class LanguageService {

    private final LanguageDAO languageDAO = new LanguageDAO();

    /** Returns every language, ordered by name. */
    public List<Language> getAll() {
        return languageDAO.findAll();
    }

    /** Returns the languages whose parser is available on this machine. */
    public List<Language> getSupported() {
        List<Language> supported = new ArrayList<>();
        for (Language language : languageDAO.findAll()) {
            if (ParserRegistry.isSupported(language.getParserType())) {
                supported.add(language);
            }
        }
        return supported;
    }

    /** Returns the language with the given id, or {@code null}. */
    public Language getById(int id) {
        return languageDAO.findById(id);
    }

    /** Returns the language with the given name, ignoring the case, or {@code null}. */
    public Language getByName(String name) {
        return languageDAO.findByName(name);
    }

    /**
     * Creates or updates a language.
     *
     * @return the id of the language, or zero when the language is not valid
     */
    public int save(Language language) {
        String error = validate(language);
        if (error != null) {
            return 0;
        }
        if (language.getId() > 0) {
            languageDAO.update(language);
            return language.getId();
        }
        return languageDAO.insert(language);
    }

    /** Deletes a language and everything attached to it. */
    public boolean delete(int id) {
        return languageDAO.delete(id);
    }

    /**
     * Validates a language.
     *
     * @return the reason why the language is not valid, or {@code null}
     */
    public String validate(Language language) {
        if (language.getName() == null || language.getName().trim().isEmpty()) {
            return "The name of the language is required.";
        }
        Language existing = languageDAO.findByName(language.getName().trim());
        if (existing != null && existing.getId() != language.getId()) {
            return "Language " + language.getName().trim() + " already exists.";
        }
        if (!ParserRegistry.isSupported(language.getParserType())) {
            return "The parser of the language is not available.";
        }
        for (LanguageDictionary dictionary : language.getDictionaries()) {
            if (dictionary.getDictUri() == null || dictionary.getDictUri().isBlank()) {
                return "A dictionary without an address cannot be saved.";
            }
        }
        return null;
    }

    /**
     * Builds the URL used to look a word up in a dictionary.
     *
     * @param dictionary the dictionary to use
     * @param text       the text to look up
     */
    public String buildDictionaryUrl(LanguageDictionary dictionary, String text) {
        String url = dictionary.getDictUri();
        if (url == null) {
            return "";
        }
        return url.replace("[LUTE]", com.leelo.util.TextTokens.strip(text));
    }
}
