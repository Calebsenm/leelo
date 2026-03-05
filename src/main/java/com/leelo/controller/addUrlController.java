package com.leelo.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;

public class addUrlController {

    @FXML
    private TextField titleField;
    @FXML
    private TextField urlField;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator loadingIndicator;

    private addTextController parent;
    private boolean loading;

    public void setParentController(addTextController parent) {
        this.parent = parent;
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> saveUrl());
        cancelButton.setOnAction(e -> closeWindow());
        urlField.setOnAction(e -> saveUrl());
        titleField.setOnAction(e -> saveUrl());
    }

    private void saveUrl() {
        if (loading) {
            return;
        }

        String rawTitle = titleField.getText() != null ? titleField.getText().trim() : "";
        String rawUrl = urlField.getText() != null ? urlField.getText().trim() : "";

        if (rawUrl.isBlank()) {
            showStatus("Ingresa una URL para continuar.", true);
            urlField.requestFocus();
            return;
        }

        String normalizedUrl = normalizeUrl(rawUrl);
        if (!isValidHttpUrl(normalizedUrl)) {
            showStatus("La URL no es valida. Usa un enlace como https://sitio.com.", true);
            urlField.requestFocus();
            return;
        }

        urlField.setText(normalizedUrl);
        showStatus("Importando contenido...", false);
        setLoadingState(true);

        Task<FetchResult> fetchTask = new Task<>() {
            @Override
            protected FetchResult call() throws Exception {
                Document doc = Jsoup.connect(normalizedUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(12000)
                        .get();

                String extractedText = doc.body() != null ? doc.body().text() : "";
                if (extractedText == null || extractedText.isBlank()) {
                    throw new IllegalStateException("No se pudo extraer contenido legible de la URL.");
                }

                String resolvedTitle = !rawTitle.isBlank() ? rawTitle : buildFallbackTitle(doc, normalizedUrl);
                return new FetchResult(resolvedTitle, extractedText);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            setLoadingState(false);
            FetchResult result = fetchTask.getValue();
            if (parent != null) {
                parent.addTextFromUrl(result.title(), result.content());
            }
            closeWindow();
        });

        fetchTask.setOnFailed(e -> {
            setLoadingState(false);
            Throwable ex = fetchTask.getException();
            String message = ex != null && ex.getMessage() != null
                    ? ex.getMessage()
                    : "No se pudo leer la pagina. Intenta con otra URL.";
            showStatus(message, true);
        });

        Thread worker = new Thread(fetchTask, "url-import-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private boolean isValidHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null
                    && !host.isBlank();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String buildFallbackTitle(Document doc, String url) {
        String docTitle = doc.title() != null ? doc.title().trim() : "";
        if (!docTitle.isBlank()) {
            return docTitle;
        }
        try {
            URI uri = new URI(url);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
        } catch (Exception ignored) {
            // Ignored on purpose.
        }
        return "Texto desde URL";
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.getStyleClass().removeAll("text-error", "text-success", "text-secondary");
        statusLabel.getStyleClass().add(error ? "text-error" : "text-secondary");
    }

    private void setLoadingState(boolean value) {
        loading = value;
        loadingIndicator.setVisible(value);
        loadingIndicator.setManaged(value);

        saveButton.setDisable(value);
        cancelButton.setDisable(value);
        titleField.setDisable(value);
        urlField.setDisable(value);
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private record FetchResult(String title, String content) {
    }
}
