package com.leelo.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryService {
    private static final String FREE_DICTIONARY_API = "https://api.dictionaryapi.dev/api/v2/entries/";
    private static final String WIKTIONARY_URL = "https://en.wiktionary.org/wiki/";
    private static final Pattern PHONETIC_PATTERN = Pattern.compile("\"phonetic\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern MEANING_BLOCK_PATTERN = Pattern.compile("\\{\\s*\"partOfSpeech\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"(.*?)\\}", Pattern.DOTALL);
    private static final Pattern DEFINITION_PATTERN = Pattern.compile("\"definition\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern EXAMPLE_PATTERN = Pattern.compile("\"example\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final ConcurrentHashMap<String, DictionaryLookupResult> LOOKUP_CACHE = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public DictionaryLookupResult lookup(String term) throws Exception {
        String cleanTerm = term == null ? "" : term.trim();
        if (cleanTerm.isEmpty()) {
            return new DictionaryLookupResult("", List.of());
        }

        String cacheKey = cleanTerm.toLowerCase();
        DictionaryLookupResult cached = LOOKUP_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        DictionaryLookupResult englishResult = fetchSafely(() -> lookupFreeDictionary(cleanTerm, "en", "Free Dictionary EN"));
        String phonetic = englishResult.phonetic();
        List<DictionaryMeaning> mergedMeanings = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        mergeMeanings(mergedMeanings, seen, englishResult.meanings());

        if (mergedMeanings.isEmpty()) {
            DictionaryLookupResult wiktionaryResult = fetchSafely(() -> lookupWiktionary(cleanTerm));
            phonetic = choosePhonetic(phonetic, wiktionaryResult.phonetic());
            mergeMeanings(mergedMeanings, seen, wiktionaryResult.meanings());
        }

        if (mergedMeanings.isEmpty()) {
            DictionaryLookupResult spanishResult = fetchSafely(() -> lookupFreeDictionary(cleanTerm, "es", "Free Dictionary ES"));
            phonetic = choosePhonetic(phonetic, spanishResult.phonetic());
            mergeMeanings(mergedMeanings, seen, spanishResult.meanings());
        }

        DictionaryLookupResult finalResult = new DictionaryLookupResult(phonetic, List.copyOf(mergedMeanings));
        LOOKUP_CACHE.put(cacheKey, finalResult);
        return finalResult;
    }

    private DictionaryLookupResult lookupFreeDictionary(String term, String languageCode, String source) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FREE_DICTIONARY_API + languageCode + "/" + URLEncoder.encode(term, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(4))
                .header("User-Agent", "Leelo/0.1.1 (desktop app)")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return new DictionaryLookupResult("", List.of());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo consultar el diccionario. Codigo: " + response.statusCode());
        }

        return parseFreeDictionaryResponse(response.body(), source);
    }

    private DictionaryLookupResult parseFreeDictionaryResponse(String json, String source) {
        String phonetic = "";
        Matcher phoneticMatcher = PHONETIC_PATTERN.matcher(json);
        if (phoneticMatcher.find()) {
            phonetic = unescapeJson(phoneticMatcher.group(1));
        }

        List<DictionaryMeaning> meanings = new ArrayList<>();
        Matcher meaningMatcher = MEANING_BLOCK_PATTERN.matcher(json);
        while (meaningMatcher.find()) {
            String partOfSpeech = unescapeJson(meaningMatcher.group(1));
            String block = meaningMatcher.group(2);

            List<String> definitions = findAll(block, DEFINITION_PATTERN);
            List<String> examples = findAll(block, EXAMPLE_PATTERN);

            int count = Math.min(definitions.size(), 3);
            for (int i = 0; i < count; i++) {
                String definition = definitions.get(i);
                String example = i < examples.size() ? examples.get(i) : "";
                meanings.add(createMeaning(source, partOfSpeech, definition, example));
            }
        }

        return new DictionaryLookupResult(phonetic, meanings);
    }

    private DictionaryLookupResult lookupWiktionary(String term) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WIKTIONARY_URL + URLEncoder.encode(term, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(4))
                .header("User-Agent", "Leelo/0.1.1 (desktop app)")
                .header("Accept", "text/html")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return new DictionaryLookupResult("", List.of());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo consultar Wiktionary. Codigo: " + response.statusCode());
        }

        return parseWiktionaryHtml(response.body());
    }

    private DictionaryLookupResult parseWiktionaryHtml(String html) {
        Document document = Jsoup.parse(html);
        Element englishHeadline = document.selectFirst("span.mw-headline#English");
        if (englishHeadline == null) {
            return new DictionaryLookupResult("", List.of());
        }

        Element current = englishHeadline.parent();
        String currentPartOfSpeech = "";
        List<DictionaryMeaning> meanings = new ArrayList<>();

        while (current != null) {
            current = current.nextElementSibling();
            if (current == null) {
                break;
            }
            if ("h2".equals(current.tagName())) {
                break;
            }

            if ("h3".equals(current.tagName()) || "h4".equals(current.tagName())) {
                Element headline = current.selectFirst("span.mw-headline");
                if (headline != null) {
                    currentPartOfSpeech = headline.text();
                }
                continue;
            }

            if (!"ol".equals(current.tagName())) {
                continue;
            }

            Elements items = current.select("> li");
            int count = Math.min(items.size(), 3);
            for (int i = 0; i < count; i++) {
                Element item = items.get(i).clone();
                item.select("ul, ol, dl").remove();
                String definition = item.text().trim();
                if (definition.isEmpty()) {
                    continue;
                }
                meanings.add(createMeaning("Wiktionary", currentPartOfSpeech, definition, ""));
            }
        }

        return new DictionaryLookupResult("", meanings);
    }

    private DictionaryMeaning createMeaning(String source, String partOfSpeech, String definition, String example) {
        String cleanPartOfSpeech = partOfSpeech == null ? "" : partOfSpeech.trim();
        String cleanDefinition = definition == null ? "" : definition.trim();
        String cleanExample = example == null ? "" : example.trim();
        String suggestedMeaning = cleanPartOfSpeech.isBlank()
                ? cleanDefinition
                : cleanPartOfSpeech + ": " + cleanDefinition;
        return new DictionaryMeaning(source, cleanPartOfSpeech, cleanDefinition, cleanExample, suggestedMeaning);
    }

    private void mergeMeanings(List<DictionaryMeaning> target, Set<String> seen, List<DictionaryMeaning> incoming) {
        for (DictionaryMeaning meaning : incoming) {
            String key = (meaning.partOfSpeech() + "|" + meaning.definition()).trim().toLowerCase();
            if (key.isBlank() || !seen.add(key)) {
                continue;
            }
            target.add(meaning);
        }
    }

    private String choosePhonetic(String current, String candidate) {
        if (current != null && !current.isBlank()) {
            return current;
        }
        return candidate == null ? "" : candidate.trim();
    }

    private List<String> findAll(String text, Pattern pattern) {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(unescapeJson(matcher.group(1)));
        }
        return values;
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\/", "/")
                .replace("\\\\", "\\")
                .trim();
    }

    private DictionaryLookupResult fetchSafely(ProviderCall providerCall) {
        try {
            return providerCall.call();
        } catch (Exception ignored) {
            return new DictionaryLookupResult("", List.of());
        }
    }

    @FunctionalInterface
    private interface ProviderCall {
        DictionaryLookupResult call() throws Exception;
    }

    public record DictionaryLookupResult(String phonetic, List<DictionaryMeaning> meanings) {
    }

    public record DictionaryMeaning(String source, String partOfSpeech, String definition, String example, String suggestedMeaning) {
    }
}
