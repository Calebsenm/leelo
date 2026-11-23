package com.leelo.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leelo.model.AiDefinition;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to interact with a local Ollama instance for automatic translation,
 * definitions, and example sentences.
 */
public class OllamaService {

    private final SettingService settingService;
    private final HttpClient httpClient;
    private final Gson gson;

    public OllamaService() {
        this(new SettingService(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public OllamaService(SettingService settingService, HttpClient httpClient) {
        this.settingService = settingService;
        this.httpClient = httpClient;
        this.gson = new com.google.gson.GsonBuilder().setLenient().create();
    }

    /** Returns the configured base URL for Ollama. */
    public String getBaseUrl() {
        String url = settingService.getRaw(SettingService.OLLAMA_URL);
        if (url == null || url.isBlank()) {
            url = "http://localhost:11434";
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /** Returns the configured model name for Ollama. */
    public String getModel() {
        String model = settingService.getRaw(SettingService.OLLAMA_MODEL);
        if (model == null || model.isBlank() || "llama3.2".equalsIgnoreCase(model.trim())) {
            return "qwen2.5:3b";
        }
        return model.trim();
    }

    /** Checks whether Ollama is accessible at the configured URL. */
    public boolean isOllamaRunning() {
        return isOllamaRunning(getBaseUrl());
    }

    /** Checks whether Ollama is accessible at the given URL. */
    public boolean isOllamaRunning(String baseUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns the list of installed models on the Ollama instance. */
    public List<String> getInstalledModels(String baseUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Ollama retornó código " + response.statusCode());
        }
        List<String> modelNames = new ArrayList<>();
        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        if (json != null && json.has("models")) {
            JsonArray models = json.getAsJsonArray("models");
            for (JsonElement el : models) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("name")) {
                        modelNames.add(obj.get("name").getAsString());
                    }
                }
            }
        }
        return modelNames;
    }

    /**
     * Generates a rich bilingual definition for a vocabulary term using Ollama.
     *
     * @param term the word or multi-word term
     * @param contextSentence optional sentence where the term appears
     * @return the structured AI definition
     * @throws Exception if connection fails, model is missing, or parsing fails
     */
    public AiDefinition generateDefinition(String term, String contextSentence) throws Exception {
        String baseUrl = getBaseUrl();
        String model = getModel();

        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("stream", false);
        root.addProperty("format", "json");
        root.addProperty("keep_alive", "60m");

        JsonObject options = new JsonObject();
        options.addProperty("num_ctx", 512);
        options.addProperty("num_predict", 250);
        options.addProperty("temperature", 0.1);
        root.add("options", options);

        JsonArray messages = new JsonArray();

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content",
                "You are an expert bilingual language dictionary and tutor. "
                + "Analyze the given English term and context. "
                + "You must respond strictly with a valid JSON object matching this schema: "
                + "{\n"
                + "  \"translation\": \"<Spanish translation of the term in context>\",\n"
                + "  \"english_definition\": \"<clear and concise definition in English>\",\n"
                + "  \"spanish_definition\": \"<Spanish translation of the English definition>\",\n"
                + "  \"english_example\": \"<a natural example sentence in English using the term>\"\n"
                + "}\n"
                + "Do NOT wrap in markdown blocks. Return ONLY the JSON object.");
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        StringBuilder userContent = new StringBuilder();
        userContent.append("Term: \"").append(term).append("\"");
        if (contextSentence != null && !contextSentence.isBlank()) {
            userContent.append("\nSentence Context: \"").append(contextSentence.trim()).append("\"");
        }
        userMsg.addProperty("content", userContent.toString());
        messages.add(userMsg);

        root.add("messages", messages);

        String requestJson = gson.toJson(root);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.ConnectException e) {
            throw new IllegalStateException("No se pudo conectar a Ollama en " + baseUrl + ". Asegúrate de ejecutar 'ollama serve'.", e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IllegalStateException("La consulta a Ollama tardó demasiado (tiempo de espera agotado).", e);
        }

        if (response.statusCode() == 404) {
            throw new IllegalStateException("Modelo '" + model + "' no encontrado en Ollama. Ejecuta: ollama pull " + model);
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Ollama retornó código " + response.statusCode() + ": " + response.body());
        }

        return parseChatResponse(response.body());
    }

    /** Parses the JSON response from Ollama's /api/chat endpoint. */
    public AiDefinition parseChatResponse(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        if (json == null || !json.has("message")) {
            throw new IllegalStateException("Respuesta de Ollama inválida: falta el campo 'message'.");
        }
        JsonObject message = json.getAsJsonObject("message");
        if (!message.has("content")) {
            throw new IllegalStateException("Respuesta de Ollama inválida: falta el campo 'content'.");
        }
        String content = message.get("content").getAsString().trim();

        return parseDefinitionJson(content);
    }

    /** Parses the inner JSON string containing the definition fields. */
    public AiDefinition parseDefinitionJson(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            JsonObject obj = gson.fromJson(cleaned, JsonObject.class);
            if (obj != null) {
                String translation = getStringField(obj, "translation", "traduccion");
                String enDef = getStringField(obj, "english_definition", "englishDefinition", "definition_en", "definition");
                String esDef = getStringField(obj, "spanish_definition", "spanishDefinition", "definition_es", "traduccion_definicion");
                String example = getStringField(obj, "english_example", "englishExample", "example_en", "example", "ejemplo");
                if (!translation.isBlank() || !enDef.isBlank() || !esDef.isBlank() || !example.isBlank()) {
                    return new AiDefinition(translation, enDef, esDef, example);
                }
            }
        } catch (Exception e) {
            // If JSON was cut off or malformed, gracefully recover using regex
        }

        return extractWithRegex(cleaned);
    }

    /** Extracts dictionary fields from raw or truncated JSON text using regex. */
    public AiDefinition extractWithRegex(String text) {
        String translation = extractRegexField(text, "translation", "traduccion");
        String enDef = extractRegexField(text, "english_definition", "englishDefinition", "definition_en", "definition");
        String esDef = extractRegexField(text, "spanish_definition", "spanishDefinition", "definition_es", "traduccion_definicion");
        String example = extractRegexField(text, "english_example", "englishExample", "example_en", "example", "ejemplo");

        if (translation.isBlank() && enDef.isBlank() && esDef.isBlank() && example.isBlank()) {
            return new AiDefinition(text.replaceAll("[{}\"\\[\\]]", "").trim(), "", "", "");
        }

        return new AiDefinition(translation, enDef, esDef, example);
    }

    private String extractRegexField(String text, String... keys) {
        for (String key : keys) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"\\r\\n]*)\"?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim().replace("\\\"", "\"").replace("\\n", " ");
            }
        }
        return "";
    }

    private String getStringField(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        }
        return "";
    }
}
