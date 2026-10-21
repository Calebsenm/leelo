package com.leelo.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the available parsers.
 *
 * <p>Languages reference a parser by its type key, which keeps the stored data
 * independent from the implementation.
 */
public final class ParserRegistry {

    private static final Map<String, TextParser> PARSERS = new LinkedHashMap<>();

    static {
        register(new SpaceDelimitedParser());
        register(new TurkishParser());
        register(new ClassicalChineseParser());
    }

    private ParserRegistry() {
    }

    private static void register(TextParser parser) {
        PARSERS.put(parser.getType(), parser);
    }

    /** Returns the parser for the given type, or {@code null} when unknown. */
    public static TextParser getParser(String type) {
        return type == null ? null : PARSERS.get(type);
    }

    /** Returns the parser for the given type, falling back to the space delimited one. */
    public static TextParser getParserOrDefault(String type) {
        TextParser parser = getParser(type);
        return parser != null ? parser : PARSERS.get(SpaceDelimitedParser.TYPE);
    }

    /** True when the given parser type is known and usable. */
    public static boolean isSupported(String type) {
        TextParser parser = getParser(type);
        return parser != null && parser.isSupported();
    }

    /** Returns the types of every supported parser. */
    public static List<String> getSupportedTypes() {
        List<String> types = new ArrayList<>();
        for (TextParser parser : PARSERS.values()) {
            if (parser.isSupported()) {
                types.add(parser.getType());
            }
        }
        return types;
    }

    /** Returns the display names of every supported parser, keyed by type. */
    public static Map<String, String> getSupportedNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (TextParser parser : PARSERS.values()) {
            if (parser.isSupported()) {
                names.put(parser.getType(), parser.getName());
            }
        }
        return names;
    }
}
