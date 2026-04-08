package com.leelo.controller;

import com.leelo.model.Texts;
import com.leelo.model.Word;
import com.leelo.service.ImageSearchService;
import com.leelo.service.TextService;
import com.leelo.service.WordService;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadingController {
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageLabel;
    @FXML
    private Button decreaseFontButton;
    @FXML
    private Button increaseFontButton;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox textVBox;
    @FXML
    private SideMenuController menuController;

    private int currentPage = 1;
    private int totalPages = 1;
    private double fontSize = 25.0;
    private Texts currentText;
    private final WordService WordService = new WordService();
    private final ImageSearchService imageSearchService = new ImageSearchService();
    private final TextService textService = new TextService();
    private final Map<String, Word> savedWords = new HashMap<>();
    private final List<String> pages = new ArrayList<>();
    private static final int WORDS_PER_PAGE = 200;

    @FXML
    public void initialize() {
        showPage();
        prevPageButton.setOnAction(e -> previousPage());
        nextPageButton.setOnAction(e -> nextPage());
        decreaseFontButton.setOnAction(e -> adjustFont(-2));
        increaseFontButton.setOnAction(e -> adjustFont(2));
    }

    private void showPage() {
        textVBox.getChildren().clear();
        String content;
        if (pages.isEmpty()) {
            if (currentText == null || currentText.getText() == null || currentText.getText().trim().isEmpty()) {
                Label empty = new Label("No text to display. Select or add a valid text.");
                empty.setStyle("-fx-text-fill: red; -fx-font-size: " + fontSize + "px;");
                textVBox.getChildren().add(empty);
                pageLabel.setText("Page 0/0");
                return;
            }
            content = currentText.getText();
        } else {
            content = pages.get(currentPage - 1);
        }

        TextFlow flow = new TextFlow();
        loadSavedWords();

        for (String word : content.split("\\s+")) {
            String wordNorm = normalizeWord(word);
            Word info = savedWords.get(wordNorm);

            Text t = new Text(word + " ");
            t.setStyle("-fx-font-size: " + fontSize + "px;");
            applyWordColor(t, info);

            String cleanSelectedWord = cleanWordForSelection(word);
            t.setOnMouseClicked(e -> {
                Word currentInfo = savedWords.get(wordNorm);
                if (e.getButton() == MouseButton.PRIMARY) {
                    showMeaningOptions(t, cleanSelectedWord, currentInfo);
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    openWordPopup(cleanSelectedWord);
                }
            });

            flow.getChildren().add(t);
        }

        textVBox.getChildren().add(flow);
        pageLabel.setText("Page " + currentPage + "/" + totalPages);
    }

    private void applyWordColor(Text t, Word info) {
        if (info == null) {
            t.setFill(Color.rgb(0, 60, 255));
            return;
        }
        switch (info.getState()) {
            case 1:
                t.setFill(Color.rgb(0, 60, 255));
                break;
            case 2:
                t.setFill(Color.rgb(240, 161, 13));
                break;
            case 3:
                t.setFill(Color.rgb(82, 194, 8));
                break;
            case 4:
                t.setFill(Color.rgb(2, 46, 9));
                break;
            default:
                t.setFill(Color.BLACK);
                break;
        }
    }

    private String cleanWordForSelection(String word) {
        return word.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    }

    private void loadSavedWords() {
        savedWords.clear();
        for (Word p : WordService.listWords()) {
            String termNorm = normalizeWord(p.getTerm());
            savedWords.put(termNorm, p);
        }
    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            textService.updateProgress(currentText.getIdText(), currentPage);
            showPage();
        } else {
            try {
                HomeController homeController = new HomeController();
                homeController.initialize();

                BorderPane root = new BorderPane();
                FXMLLoader sideMenuLoader = new FXMLLoader(getClass().getResource("/com/leelo/side_menu.fxml"));
                VBox sideMenu = sideMenuLoader.load();

                root.setLeft(sideMenu);
                root.setCenter(homeController.getView());

                com.leelo.App.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            textService.updateProgress(currentText.getIdText(), currentPage);
            showPage();
        } else {
            try {
                HomeController homeController = new HomeController();
                homeController.initialize();

                BorderPane root = new BorderPane();
                FXMLLoader sideMenuLoader = new FXMLLoader(getClass().getResource("/com/leelo/side_menu.fxml"));
                VBox sideMenu = sideMenuLoader.load();

                root.setLeft(sideMenu);
                root.setCenter(homeController.getView());

                com.leelo.App.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void adjustFont(double delta) {
        fontSize = Math.max(10, fontSize + delta);
        showPage();
    }

    public void setText(Texts selected) {
        this.currentText = selected;
        preparePages();

        int idSelectedText = selected.getIdText();
        currentPage = textService.getPage(idSelectedText);
        if (currentPage == 0) {
            currentPage = 1;
        }

        textService.updateProgress(currentText.getIdText(), currentPage);
        totalPages = pages.size();
        showPage();
    }

    private void preparePages() {
        pages.clear();
        if (currentText == null) {
            return;
        }
        String[] words = currentText.getText().split("\\s+");
        StringBuilder page = new StringBuilder();
        int count = 0;
        for (String word : words) {
            page.append(word).append(" ");
            count++;
            if (count >= WORDS_PER_PAGE) {
                pages.add(page.toString().trim());
                page = new StringBuilder();
                count = 0;
            }
        }
        if (page.length() > 0) {
            pages.add(page.toString().trim());
        }
    }

    private void openWordPopup(String word) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/leelo/add_word.fxml"));
            Parent root = loader.load();

            addWordController controller = loader.getController();
            Word info = savedWords.get(normalizeWord(word));
            if (info != null) {
                controller.setWordToEdit(info);
            } else {
                controller.setWordToEdit(null);
                controller.setDefaultTerm(word);
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Palabra");
            dialog.setScene(new Scene(root, 420, 540));
            dialog.showAndWait();

            loadSavedWords();
            showPage();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String normalizeWord(String word) {
        String withoutPunctuation = word.replaceAll("[^\\p{L}]", "").toLowerCase();
        return Normalizer.normalize(withoutPunctuation, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private List<String> parseMeanings(String rawTranslation) {
        List<String> meanings = new ArrayList<>();
        if (rawTranslation == null || rawTranslation.trim().isEmpty()) {
            return meanings;
        }

        String[] parts = rawTranslation.split("\\r?\\n|\\||;|,|/");
        Set<String> unique = new LinkedHashSet<>();
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                unique.add(cleaned);
            }
        }

        meanings.addAll(unique);
        if (meanings.isEmpty() && !rawTranslation.trim().isEmpty()) {
            meanings.add(rawTranslation.trim());
        }
        return meanings;
    }

    private void showMeaningOptions(Text textNode, String selectedWord, Word info) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        HBox box = new HBox(12);
        box.setStyle(
                "-fx-background-color: white;" +
                "-fx-padding: 12;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #d8e0ef;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 12, 0, 0, 3);" +
                "-fx-max-width: 460;");

        VBox detailsBox = new VBox(6);
        detailsBox.setStyle("-fx-max-width: 260;");

        Label title = new Label(selectedWord);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2a44;");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleRow.getChildren().add(title);

        Button speakButton = new Button();
        speakButton.setGraphic(createSpeakerIcon());
        speakButton.setStyle(
                "-fx-background-color: #eff6ff;" +
                "-fx-border-color: #bfdbfe;" +
                "-fx-border-radius: 999;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 5 7;" +
                "-fx-cursor: hand;");
        speakButton.setOnAction(e -> speakText(selectedWord));
        titleRow.getChildren().add(speakButton);

        detailsBox.getChildren().add(titleRow);

        String pronunciation = info != null ? info.getPronunciation() : null;
        if (pronunciation != null && !pronunciation.trim().isEmpty()) {
            Label pronunciationLabel = new Label(pronunciation.trim());
            pronunciationLabel.setWrapText(true);
            pronunciationLabel.setStyle(
                    "-fx-font-size: 12px;" +
                    "-fx-font-style: italic;" +
                    "-fx-text-fill: #475569;");
            detailsBox.getChildren().add(pronunciationLabel);
        }

        List<String> meanings = info != null ? parseMeanings(info.getTranslation()) : new ArrayList<>();

        if (meanings.isEmpty()) {
            Label empty = new Label("No hay significados guardados.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #5d6a85;");
            detailsBox.getChildren().add(empty);
        } else {
            int maxVisible = 3;
            int total = meanings.size();
            int visible = Math.min(maxVisible, total);

            for (int i = 0; i < visible; i++) {
                String meaning = meanings.get(i);
                Label option = new Label(meaning);
                option.setWrapText(true);
                option.setMaxWidth(236);
                option.setStyle(
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #2a3550;" +
                        "-fx-padding: 7 10;" +
                        "-fx-background-color: #f5f8ff;" +
                        "-fx-border-color: #d9e4ff;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;");
                detailsBox.getChildren().add(option);
            }

            if (total > maxVisible) {
                Label more = new Label("+" + (total - maxVisible) + " mas");
                more.setStyle("-fx-font-size: 11px; -fx-text-fill: #5d6a85;");
                detailsBox.getChildren().add(more);
            }

        }

        Button editButton = new Button(meanings.isEmpty() ? "Agregar significados" : "Editar significados");
        editButton.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-padding: 7 10;" +
                "-fx-background-color: #2563eb;" +
                "-fx-border-color: #2563eb;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
        editButton.setOnAction(e -> {
            popup.hide();
            openWordPopup(selectedWord);
        });
        detailsBox.getChildren().add(editButton);

        box.getChildren().add(detailsBox);

        String imageUrl = info != null ? info.getUrlImg() : null;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            box.getChildren().add(createImagePanel(imageUrl.trim()));
        }

        popup.getContent().add(box);

        Point2D p = textNode.localToScreen(0, textNode.getBoundsInLocal().getHeight() + 8);
        popup.show(textNode, p.getX(), p.getY());

        PauseTransition delay = new PauseTransition(Duration.seconds(8));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }

    private StackPane createImagePanel(String imageUrl) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Label loadingLabel = new Label("Cargando imagen...");
        loadingLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        StackPane imagePane = new StackPane(imageView, loadingLabel);
        imagePane.setPrefSize(170, 150);
        imagePane.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-border-color: #d8e0ef;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 8;");

        Task<Image> imageTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return imageSearchService.downloadImage(imageUrl);
            }
        };
        imageTask.setOnSucceeded(e -> {
            imageView.setImage(imageTask.getValue());
            loadingLabel.setVisible(false);
        });
        imageTask.setOnFailed(e -> loadingLabel.setText("No se pudo cargar."));

        Thread worker = new Thread(imageTask, "reading-image-download-task");
        worker.setDaemon(true);
        worker.start();

        return imagePane;
    }

    private SVGPath createSpeakerIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M3 10v4h3l4 3V7L6 10H3zm10.5 2a3.5 3.5 0 0 0-2-3.15v6.29A3.5 3.5 0 0 0 13.5 12zm0-7a.75.75 0 0 0-.38 1.4A6.98 6.98 0 0 1 17 12a6.98 6.98 0 0 1-3.88 6.6.75.75 0 1 0 .76 1.3A8.48 8.48 0 0 0 18.5 12 8.48 8.48 0 0 0 13.88 4.1.75.75 0 0 0 13.5 4z");
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);
        icon.setStyle("-fx-fill: #1d4ed8;");
        return icon;
    }

    private void speakText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                String safeText = text.replace("'", "''");
                String command =
                        "$speaker = New-Object -ComObject SAPI.SpVoice; " +
                        "$englishVoice = $speaker.GetVoices() | Where-Object { " +
                        "$desc = $_.GetDescription(); " +
                        "$desc -match 'English|US|UK|en-' " +
                        "} | Select-Object -First 1; " +
                        "if ($englishVoice) { $speaker.Voice = $englishVoice; } " +
                        "$speaker.Speak('" + safeText + "') | Out-Null";
                ProcessBuilder builder = new ProcessBuilder("powershell", "-NoProfile", "-Command", command);
                builder.start().waitFor();
            } catch (Exception ignored) {
            }
        }, "leelo-pronunciation-speaker");
        worker.setDaemon(true);
        worker.start();
    }
}
