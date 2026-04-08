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

public class ImageSearchService {
    private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<ImageSearchResult> searchImages(String term, int limit) throws Exception {
        String cleanTerm = term == null ? "" : term.trim();
        if (cleanTerm.isEmpty()) {
            return List.of();
        }

        String query = COMMONS_API
                + "?action=query"
                + "&generator=search"
                + "&gsrsearch=" + URLEncoder.encode(cleanTerm, StandardCharsets.UTF_8)
                + "&gsrnamespace=6"
                + "&gsrlimit=" + Math.max(1, Math.min(limit, 40))
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
            throw new IllegalStateException("No se pudo consultar imagenes. Codigo: " + response.statusCode());
        }

        return parseResults(response.body());
    }

    private List<ImageSearchResult> parseResults(String xml) throws Exception {
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
            String title = page.getAttribute("title");
            String storageUrl = chooseStorageUrl(thumbUrl, fullUrl);

            if (storageUrl == null || storageUrl.isBlank() || !seen.add(storageUrl)) {
                continue;
            }

            results.add(new ImageSearchResult(title, thumbUrl, fullUrl, storageUrl));
        }

        return results;
    }

    public Image downloadImage(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL vacia");
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

        return new Image(response.body());
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

    public record ImageSearchResult(String title, String thumbnailUrl, String fullUrl, String storageUrl) {
    }
}
