package com.leelo.parse;

import com.leelo.model.Language;

import java.util.Locale;

/**
 * Parser for Turkish, which differs from the standard space-delimited parser by
 * the way the dotted and dotless "i" are cased.
 */
public class TurkishParser extends SpaceDelimitedParser {

    public static final String TYPE = "turkish";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "Turkish";
    }

    /**
     * Lower-cases the text, mapping the capital dotted "I" to the dotless "ı"
     * and the capital "İ" to the dotted "i".
     */
    @Override
    public String getLowercase(String text, Language language) {
        if (text == null) {
            return "";
        }
        return text.replace("İ", "i").replace("I", "ı").toLowerCase(Locale.ROOT);
    }
}
