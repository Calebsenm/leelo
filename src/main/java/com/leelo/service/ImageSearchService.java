package com.leelo.service;

import javafx.scene.image.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageSearchService {
    private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";
    private static final String PEXELS_API = "https://api.pexels.com/v1/search";
    private static final String PIXABAY_API = "https://pixabay.com/api/";
    private static final String PEXELS_API_KEY = readApiKey("PEXELS_API_KEY");
    private static final String PIXABAY_API_KEY = readApiKey("PIXABAY_API_KEY");
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");
    private static final Pattern PIXABAY_HIT_PATTERN = Pattern.compile("\\{[^{}]*\"webformatURL\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"[^{}]*\"largeImageURL\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"[^{}]*\"tags\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"[^{}]*\\}");
    private static final Pattern PEXELS_PHOTO_PATTERN = Pattern.compile("\"alt\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\".*?\"src\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern PEXELS_SRC_MEDIUM_PATTERN = Pattern.compile("\"medium\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern PEXELS_SRC_LARGE_PATTERN = Pattern.compile("\"large2x\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"|\"large\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final ConcurrentHashMap<String, List<ImageSearchResult>> SEARCH_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "leelo-image-service");
        thread.setDaemon(true);
        return thread;
    });

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<ImageSearchResult> searchImages(String term, int limit) throws Exception {
        String cleanTerm = term == null ? "" : term.trim();
        int safeLimit = Math.max(1, Math.min(limit, 40));
        if (cleanTerm.isEmpty()) {
            return List.of();
        }

        String cacheKey = cleanTerm.toLowerCase() + "|" + safeLimit;
        List<ImageSearchResult> cached = SEARCH_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<CompletableFuture<List<ImageSearchResult>>> tasks = new ArrayList<>();
        tasks.add(CompletableFuture.supplyAsync(() -> fetchSafely(() -> searchCommons(cleanTerm, safeLimit)), EXECUTOR));

        if (PIXABAY_API_KEY != null) {
            tasks.add(CompletableFuture.supplyAsync(() -> fetchSafely(() -> searchPixabay(cleanTerm, safeLimit)), EXECUTOR));
        }

        if (PEXELS_API_KEY != null) {
            tasks.add(CompletableFuture.supplyAsync(() -> fetchSafely(() -> searchPexels(cleanTerm, safeLimit)), EXECUTOR));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

        List<ImageSearchResult> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (CompletableFuture<List<ImageSearchResult>> task : tasks) {
            for (ImageSearchResult result : task.join()) {
                String key = result.storageUrl() == null ? "" : result.storageUrl().trim();
                if (key.isEmpty() || !seen.add(key)) {
                    continue;
                }
                merged.add(result);
                if (merged.size() >= safeLimit) {
                    SEARCH_CACHE.put(cacheKey, List.copyOf(merged));
                    return SEARCH_CACHE.get(cacheKey);
                }
            }
        }

        SEARCH_CACHE.put(cacheKey, List.copyOf(merged));
        return SEARCH_CACHE.get(cacheKey);
    }

    public Image downloadImage(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL vacia");
        }

        Image cached = IMAGE_CACHE.get(url);
        if (cached != null) {
            return cached;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Leelo/0.1.1")
                .header("Accept", "image/*,*/*;q=0.8")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo descargar la imagen. Codigo: " + response.statusCode());
        }

        Image image = new Image(response.body());
        IMAGE_CACHE.put(url, image);
        return image;
    }

    private List<ImageSearchResult> searchCommons(String term, int limit) throws Exception {
        String query = COMMONS_API
                + "?action=query"
                + "&generator=search"
                + "&gsrsearch=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                + "&gsrnamespace=6"
                + "&gsrlimit=" + limit
                + "&prop=imageinfo"
                + "&iiprop=url"
                + "&iiurlwidth=360"
                + "&format=xml"
                + "&origin=*";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(query))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Leelo/0.1.1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo consultar Wikimedia. Codigo: " + response.statusCode());
        }

        return parseCommonsResults(response.body());
    }

    private List<ImageSearchResult> parseCommonsResults(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        NodeList pageNodes = document.getElementsByTagName("page");
        List<ImageSearchResult> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (int i = 0; i < pageNodes.getLength(); i++) {
            Element page = (Element) pageNodes.item(i);
            NodeList imageInfoNodes = page.getElementsByTagName("ii");
            if (imageInfoNodes.getLength() == 0) {
                continue;
            }

            Element imageInfo = (Element) imageInfoNodes.item(0);
            String fullUrl = imageInfo.getAttribute("url");
            String thumbUrl = imageInfo.hasAttribute("thumburl") ? imageInfo.getAttribute("thumburl") : fullUrl;
            String title = cleanText(page.getAttribute("title")).replace("File:", "");
            String storageUrl = chooseStorageUrl(thumbUrl, fullUrl);

            if (storageUrl == null || storageUrl.isBlank() || !seen.add(storageUrl)) {
                continue;
            }

            results.add(new ImageSearchResult("Wikimedia", title, thumbUrl, fullUrl, storageUrl));
        }

        return results;
    }

    private List<ImageSearchResult> searchPixabay(String term, int limit) throws Exception {
        String query = PIXABAY_API
                + "?key=" + URLEncoder.encode(PIXABAY_API_KEY, StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                + "&lang=es"
                + "&image_type=photo"
                + "&safesearch=true"
                + "&per_page=" + limit;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(query))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Leelo/0.1.1")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo consultar Pixabay. Codigo: " + response.statusCode());
        }

        List<ImageSearchResult> results = new ArrayList<>();
        Matcher matcher = PIXABAY_HIT_PATTERN.matcher(response.body());
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String thumbUrl = unescapeJson(matcher.group(1));
            String fullUrl = unescapeJson(matcher.group(2));
            String title = cleanText(unescapeJson(matcher.group(3)));
            String storageUrl = chooseStorageUrl(thumbUrl, fullUrl);
            if (storageUrl == null || storageUrl.isBlank() || !seen.add(storageUrl)) {
                continue;
            }
            results.add(new ImageSearchResult("Pixabay", title, thumbUrl, fullUrl, storageUrl));
        }
        return results;
    }

    private List<ImageSearchResult> searchPexels(String term, int limit) throws Exception {
        String query = PEXELS_API
                + "?query=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                + "&per_page=" + limit
                + "&locale=es-ES";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(query))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Leelo/0.1.1")
                .header("Authorization", PEXELS_API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo consultar Pexels. Codigo: " + response.statusCode());
        }

        List<ImageSearchResult> results = new ArrayList<>();
        Matcher matcher = PEXELS_PHOTO_PATTERN.matcher(response.body());
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String title = cleanText(unescapeJson(matcher.group(1)));
            String srcBlock = matcher.group(2);
            String thumbUrl = extractFirst(srcBlock, PEXELS_SRC_MEDIUM_PATTERN);
            String fullUrl = extractLarge(srcBlock, thumbUrl);
            String storageUrl = chooseStorageUrl(thumbUrl, fullUrl);
            if (storageUrl == null || storageUrl.isBlank() || !seen.add(storageUrl)) {
                continue;
            }
            results.add(new ImageSearchResult("Pexels", title.isBlank() ? "Pexels" : title, thumbUrl, fullUrl, storageUrl));
        }
        return results;
    }

    private String chooseStorageUrl(String thumbUrl, String fullUrl) {
        if (isSupportedImageUrl(thumbUrl)) {
            return thumbUrl;
        }
        if (isSupportedImageUrl(fullUrl)) {
            return fullUrl;
        }
        return thumbUrl != null && !thumbUrl.isBlank() ? thumbUrl : fullUrl;
    }

    private boolean isSupportedImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String normalized = url.toLowerCase();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        for (String extension : SUPPORTED_EXTENSIONS) {
            if (normalized.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private String extractLarge(String srcBlock, String fallback) {
        Matcher matcher = PEXELS_SRC_LARGE_PATTERN.matcher(srcBlock);
        if (matcher.find()) {
            String large2x = matcher.group(1);
            String large = matcher.group(2);
            return cleanText(unescapeJson(large2x != null ? large2x : large));
        }
        return fallback;
    }

    private String extractFirst(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return cleanText(unescapeJson(matcher.group(1)));
        }
        return "";
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('_', ' ').trim();
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    private List<ImageSearchResult> fetchSafely(ProviderCall providerCall) {
        try {
            return providerCall.call();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String readApiKey(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    @FunctionalInterface
    private interface ProviderCall {
        List<ImageSearchResult> call() throws Exception;
    }

    public record ImageSearchResult(String source, String title, String thumbnailUrl, String fullUrl, String storageUrl) {
    }
}
