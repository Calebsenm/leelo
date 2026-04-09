package com.leelo.service;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class ImageSearchService {
    private static final ConcurrentHashMap<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
}
