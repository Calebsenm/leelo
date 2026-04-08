package com.leelo.controller;

import com.leelo.model.Word;
import com.leelo.service.ImageSearchService;
import com.leelo.service.WordService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class addWordController {
    @FXML public TextField termField;
    @FXML private Label titleLabel;
    @FXML private TextField pronunciationField;
    @FXML private ComboBox<String> stateCombo;
    @FXML private TextField urlImgField;
    @FXML private Button searchImageButton;
    @FXML private Button saveButton;
    @FXML private Label messageLabel;
    @FXML private VBox meaningsContainer;
    @FXML private Button addMeaningButton;
    @FXML private ImageView imagePreview;

    private final WordService WordService = new WordService();
    private final ImageSearchService imageSearchService = new ImageSearchService();
    private Word wordToEdit = null;

    @FXML
    public void initialize() {
        stateCombo.setItems(FXCollections.observableArrayList("New", "Learning", "Learned", "Mastered"));
        stateCombo.setValue("Learning");
        addMeaningButton.setOnAction(e -> addMeaningField(""));
        saveButton.setOnAction(e -> saveOrUpdateWord());
        searchImageButton.setGraphic(createSearchIcon());
        searchImageButton.setOnAction(e -> openImageSearchDialog());
        searchImageButton.setStyle(
                "-fx-background-color: #eff6ff;" +
                "-fx-border-color: #bfdbfe;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
        urlImgField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview(newValue));
        ensureAtLeastOneMeaningField();
        refreshPreview(urlImgField.getText());
    }

    public void setDefaultTerm(String term) {
        titleLabel.setText("Nueva palabra");
        if (wordToEdit == null && term != null && !term.trim().isEmpty()) {
            termField.setText(term.trim());
        }
    }

    public void setWordToEdit(Word word) {
        this.wordToEdit = word;
        if (word != null) {
            titleLabel.setText("Editar palabra");
            termField.setText(word.getTerm());
            pronunciationField.setText(word.getPronunciation());
            stateCombo.setValue(stateToString(word.getState()));
            urlImgField.setText(word.getUrlImg());
            this.wordToEdit.setIdTerm(word.getIdTerm());
            loadMeaningFields(word.getTranslation());
        } else {
            titleLabel.setText("Nueva palabra");
            urlImgField.clear();
            refreshPreview("");
        }
    }

    private void saveOrUpdateWord() {
        String term = termField.getText() != null ? termField.getText().trim() : "";
        String translation = collectMeanings();
        String pronunciation = pronunciationField.getText();
        String stateStr = stateCombo.getValue();
        String urlImg = urlImgField.getText();

        if (term.isEmpty() || stateStr == null || translation.isEmpty()) {
            showMessage("Completa termino, significados y estado.", true);
            return;
        }

        int state = stringToState(stateStr);
        boolean ok;

        if (wordToEdit != null) {
            wordToEdit.setTerm(term);
            wordToEdit.setTranslation(translation);
            wordToEdit.setPronunciation(pronunciation);
            wordToEdit.setState(state);
            wordToEdit.setUrlImg(urlImg);
            ok = WordService.updateWord(wordToEdit);
        } else {
            Word word = new Word();
            word.setTerm(term);
            word.setTranslation(translation);
            word.setPronunciation(pronunciation);
            word.setState(state);
            word.setUrlImg(urlImg);
            ok = WordService.addWord(word);
        }

        if (ok) {
            showMessage("Guardado correctamente", false);
            javafx.scene.Node node = saveButton;
            javafx.stage.Window window = node.getScene().getWindow();
            window.hide();
        } else {
            showMessage("No se pudo guardar la palabra.", true);
        }
    }

    private void loadMeaningFields(String translation) {
        meaningsContainer.getChildren().clear();
        List<String> meanings = parseMeanings(translation);
        if (meanings.isEmpty()) {
            addMeaningField("");
            return;
        }
        for (String meaning : meanings) {
            addMeaningField(meaning);
        }
    }

    private List<String> parseMeanings(String raw) {
        List<String> meanings = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return meanings;
        }
        String[] parts = raw.split("\\r?\\n|\\||;|,");
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                meanings.add(cleaned);
            }
        }
        if (meanings.isEmpty() && !raw.trim().isEmpty()) {
            meanings.add(raw.trim());
        }
        return meanings;
    }

    private String collectMeanings() {
        List<String> meanings = new ArrayList<>();
        for (javafx.scene.Node node : meaningsContainer.getChildren()) {
            if (!(node instanceof HBox)) {
                continue;
            }
            HBox row = (HBox) node;
            for (javafx.scene.Node child : row.getChildren()) {
                if (child instanceof TextField) {
                    String value = ((TextField) child).getText();
                    if (value != null && !value.trim().isEmpty()) {
                        meanings.add(value.trim());
                    }
                }
            }
        }
        return String.join(" | ", meanings);
    }

    private void ensureAtLeastOneMeaningField() {
        if (meaningsContainer.getChildren().isEmpty()) {
            addMeaningField("");
        }
    }

    private void addMeaningField(String value) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);

        TextField meaningField = new TextField(value);
        meaningField.setPromptText("Significado");
        meaningField.setMaxWidth(Double.MAX_VALUE);
        meaningField.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-border-color: #d1d5db;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 9 12;");
        HBox.setHgrow(meaningField, Priority.ALWAYS);

        Button removeButton = new Button("Eliminar");
        removeButton.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 600;" +
                "-fx-text-fill: #b91c1c;" +
                "-fx-padding: 7 10;" +
                "-fx-background-color: #fef2f2;" +
                "-fx-border-color: #fecaca;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
        removeButton.setOnAction(e -> {
            meaningsContainer.getChildren().remove(row);
            ensureAtLeastOneMeaningField();
        });

        row.getChildren().addAll(meaningField, removeButton);
        meaningsContainer.getChildren().add(row);
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    private SVGPath createSearchIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M15.5 14h-.79l-.28-.27a6 6 0 1 0-1.06 1.06l.27.28v.79L20 21.49 21.49 20zM10 14a4 4 0 1 1 0-8 4 4 0 0 1 0 8z");
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);
        icon.setStyle("-fx-fill: #1d4ed8;");
        return icon;
    }

    private void openImageSearchDialog() {
        String term = termField.getText() != null ? termField.getText().trim() : "";
        if (term.isEmpty()) {
            showMessage("Escribe la palabra antes de buscar imagenes.", true);
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(saveButton.getScene().getWindow());
        dialog.setTitle("Buscar imagen");

        Label title = new Label("Imagenes para: " + term);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1f2937;");

        Label statusLabel = new Label("Buscando imagenes...");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        TilePane resultsPane = new TilePane();
        resultsPane.setHgap(10);
        resultsPane.setVgap(10);
        resultsPane.setPrefColumns(3);
        resultsPane.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(resultsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(40, 40);

        StackPane centerPane = new StackPane(scrollPane, progressIndicator);
        centerPane.setPrefSize(700, 480);

        Button closeButton = new Button("Cerrar");
        closeButton.setOnAction(e -> dialog.close());

        VBox root = new VBox(12, title, statusLabel, centerPane, closeButton);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 720, 560);
        dialog.setScene(scene);

        Task<List<ImageSearchService.ImageSearchResult>> task = new Task<>() {
            @Override
            protected List<ImageSearchService.ImageSearchResult> call() throws Exception {
                return imageSearchService.searchImages(term, 30);
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            List<ImageSearchService.ImageSearchResult> results = task.getValue();
            if (results == null || results.isEmpty()) {
                statusLabel.setText("No encontre imagenes para esta palabra.");
                return;
            }

            statusLabel.setText("Haz clic en una imagen para usar su enlace.");
            for (ImageSearchService.ImageSearchResult result : results) {
                resultsPane.getChildren().add(createImageCard(result, dialog));
            }
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            Throwable error = task.getException();
            statusLabel.setText(error != null && error.getMessage() != null
                    ? error.getMessage()
                    : "Ocurrio un error al buscar imagenes.");
        });

        Thread worker = new Thread(task, "image-search-task");
        worker.setDaemon(true);
        worker.start();

        dialog.showAndWait();
    }

    private VBox createImageCard(ImageSearchService.ImageSearchResult result, Stage dialog) {
        ImageView thumbnail = new ImageView();
        thumbnail.setFitWidth(190);
        thumbnail.setFitHeight(140);
        thumbnail.setPreserveRatio(true);
        thumbnail.setSmooth(true);

        Label loadingLabel = new Label("Cargando...");
        loadingLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        StackPane imagePane = new StackPane(thumbnail, loadingLabel);
        imagePane.setPrefSize(190, 140);
        imagePane.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 8;");
        loadImageIntoView(result.thumbnailUrl(), thumbnail, loadingLabel);

        Label caption = new Label(cleanImageTitle(result.title()));
        caption.setWrapText(true);
        caption.setMaxWidth(190);
        caption.setStyle("-fx-font-size: 11px; -fx-text-fill: #334155;");

        VBox card = new VBox(6, imagePane, caption);
        card.setPadding(new Insets(8));
        card.setPrefWidth(206);
        card.setMaxWidth(206);
        card.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-border-color: #dbeafe;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;");
        card.setOnMouseClicked(e -> {
            urlImgField.setText(result.storageUrl());
            refreshPreview(result.storageUrl());
            dialog.close();
        });
        return card;
    }

    private String cleanImageTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Imagen";
        }
        return title.replace("File:", "").replace('_', ' ');
    }

    private void refreshPreview(String imageUrl) {
        String cleanUrl = imageUrl == null ? "" : imageUrl.trim();
        if (cleanUrl.isEmpty()) {
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            imagePreview.setManaged(false);
            return;
        }

        loadImageIntoView(cleanUrl, imagePreview, null);
    }

    private void loadImageIntoView(String imageUrl, ImageView imageView, Label statusLabel) {
        imageView.setImage(null);
        if (statusLabel != null) {
            statusLabel.setVisible(true);
            statusLabel.setText("Cargando...");
        }

        Task<Image> imageTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return imageSearchService.downloadImage(imageUrl);
            }
        };

        imageTask.setOnSucceeded(e -> {
            Image image = imageTask.getValue();
            imageView.setImage(image);
            imageView.setVisible(true);
            imageView.setManaged(true);
            if (imageView == imagePreview) {
                imagePreview.setVisible(true);
                imagePreview.setManaged(true);
            }
            if (statusLabel != null) {
                statusLabel.setVisible(false);
            }
        });

        imageTask.setOnFailed(e -> {
            imageView.setImage(null);
            if (imageView == imagePreview) {
                imagePreview.setVisible(false);
                imagePreview.setManaged(false);
            }
            if (statusLabel != null) {
                statusLabel.setText("No se pudo cargar");
                statusLabel.setVisible(true);
            }
        });

        Thread worker = new Thread(imageTask, "image-download-task");
        worker.setDaemon(true);
        worker.start();
    }

    private int stringToState(String state) {
        switch (state) {
            case "New": return 1;
            case "Learning": return 2;
            case "Learned": return 3;
            case "Mastered": return 4;
            default: return 2;
        }
    }

    private String stateToString(int state) {
        switch (state) {
            case 1: return "New";
            case 2: return "Learning";
            case 3: return "Learned";
            case 4: return "Mastered";
            default: return "Learning";
        }
    }
}
