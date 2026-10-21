package com.leelo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A language being studied.
 *
 * <p>A language drives how its texts are tokenised: the parser type selects the
 * tokenisation strategy while the character-level settings refine it.
 */
public class Language {

    /** Default character substitutions, in {@code from=to|from=to} form. */
    public static final String DEFAULT_CHARACTER_SUBSTITUTIONS = "´='|`='|’='|‘='|...=…|..=‥";

    /** Characters that terminate a sentence. */
    public static final String DEFAULT_SPLIT_SENTENCES = ".!?";

    /** Abbreviations that must not be treated as sentence boundaries. */
    public static final String DEFAULT_SPLIT_EXCEPTIONS = "Mr.|Mrs.|Dr.|[A-Z].|Vd.|Vds.";

    /** Characters that may form part of a word. */
    public static final String DEFAULT_WORD_CHARACTERS = "a-zA-ZÀ-ÖØ-öø-ȳáéíóúÁÉÍÓÚñÑ";

    private int id;
    private String name = "";
    private String parserType = "spacedel";
    private String characterSubstitutions = DEFAULT_CHARACTER_SUBSTITUTIONS;
    private String regexpSplitSentences = DEFAULT_SPLIT_SENTENCES;
    private String exceptionsSplitSentences = DEFAULT_SPLIT_EXCEPTIONS;
    private String wordCharacters = DEFAULT_WORD_CHARACTERS;
    private boolean rightToLeft;
    private boolean showRomanization;
    private List<LanguageDictionary> dictionaries = new ArrayList<>();
    private int bookCount;
    private String parserName = "";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParserType() {
        return parserType;
    }

    public void setParserType(String parserType) {
        this.parserType = parserType;
    }

    public String getCharacterSubstitutions() {
        return characterSubstitutions;
    }

    public void setCharacterSubstitutions(String characterSubstitutions) {
        this.characterSubstitutions = characterSubstitutions;
    }

    public String getRegexpSplitSentences() {
        return regexpSplitSentences;
    }

    public void setRegexpSplitSentences(String regexpSplitSentences) {
        this.regexpSplitSentences = regexpSplitSentences;
    }

    public String getExceptionsSplitSentences() {
        return exceptionsSplitSentences;
    }

    public void setExceptionsSplitSentences(String exceptionsSplitSentences) {
        this.exceptionsSplitSentences = exceptionsSplitSentences;
    }

    public String getWordCharacters() {
        return wordCharacters;
    }

    public void setWordCharacters(String wordCharacters) {
        this.wordCharacters = wordCharacters;
    }

    public boolean isRightToLeft() {
        return rightToLeft;
    }

    public void setRightToLeft(boolean rightToLeft) {
        this.rightToLeft = rightToLeft;
    }

    public boolean isShowRomanization() {
        return showRomanization;
    }

    public void setShowRomanization(boolean showRomanization) {
        this.showRomanization = showRomanization;
    }

    public List<LanguageDictionary> getDictionaries() {
        return dictionaries;
    }

    public void setDictionaries(List<LanguageDictionary> dictionaries) {
        this.dictionaries = dictionaries;
    }

    /** Returns the active dictionaries for the given purpose, in display order. */
    public List<LanguageDictionary> getActiveDictionaries(LanguageDictionary.UseFor useFor) {
        List<LanguageDictionary> result = new ArrayList<>();
        for (LanguageDictionary dictionary : dictionaries) {
            if (dictionary.isActive() && dictionary.getUseFor() == useFor) {
                result.add(dictionary);
            }
        }
        return result;
    }

    /** Number of books written in this language; populated by listing queries. */
    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }

    /** Display name of the parser of the language. */
    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName == null ? "" : parserName;
    }

    @Override
    public String toString() {
        return name;
    }
}
