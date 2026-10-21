package com.leelo.service;

import com.leelo.util.TextCleaner;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Extraction of the text of the documents that can be imported.
 */
public class TextImportService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** The text of a plain text file. */
    public String fromTextFile(File file) throws IOException {
        return TextCleaner.clean(Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    /** The text of a PDF file. */
    public String fromPdfFile(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new IOException("The PDF file is encrypted.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return TextCleaner.clean(stripper.getText(document));
        }
    }

    /** The title and the text of a web page. */
    public ImportedPage fromUrl(String url) throws IOException {
        String normalized = normalizeUrl(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalized))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0 (compatible; Leelo)")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("The import was interrupted.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("The page could not be read. Code: " + response.statusCode());
        }

        Document document = Jsoup.parse(response.body(), normalized);
        StringBuilder text = new StringBuilder();
        for (Element heading : document.select("h1, h2, h3, h4")) {
            appendParagraph(text, heading);
        }
        for (Element paragraph : document.select("p, li, blockquote")) {
            appendParagraph(text, paragraph);
        }
        if (text.isEmpty()) {
            text.append(document.body() == null ? "" : document.body().text());
        }

        String title = document.title() == null ? "" : document.title().trim();
        if (title.length() > 150) {
            title = title.substring(0, 150) + " ...";
        }
        if (title.isEmpty()) {
            title = normalized;
        }
        return new ImportedPage(title, TextCleaner.clean(text.toString()));
    }

    /** Appends the text of an element, keeping the paragraphs apart. */
    private void appendParagraph(StringBuilder target, Element element) {
        String content = element.text().trim();
        if (content.isEmpty()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append("\n\n");
        }
        target.append(content);
    }

    /** Adds the scheme to a URL when it is missing. */
    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    /** A page imported from a web page. */
    public record ImportedPage(String title, String content) {
    }
}
