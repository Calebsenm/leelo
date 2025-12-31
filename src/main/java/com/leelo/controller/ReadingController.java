package com.leelo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.leelo.model.Word;
import com.leelo.service.WordService;
import com.leelo.service.TextService;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import java.lang.StringBuilder;
import java.text.Normalizer;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import com.leelo.model.Texts;

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
    private WordService WordService = new WordService();
    private TextService textService = new TextService();
    private Map<String, Word> savedWords = new HashMap<>();
    private List<String> pages = new ArrayList<>();
    private static final int WORDS_PER_PAGE = 200;

    @FXML
    public void initialize() {
        // Example logic: show test text
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
            Text t = new Text(word + " ");
            t.setStyle("-fx-font-size: " + fontSize + "px;");
            Word info = savedWords.get(wordNorm);
            if (info == null) {
                t.setFill(Color.rgb(0, 60, 255));
            } else {
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
            String cleanSelectedWord = cleanWordForSelection(word);
            t.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (info == null) {
                        // Save automatically as Learning
                        saveWordAsLearning(wordNorm, cleanSelectedWord);
                    } else {
                        openWordPopup(cleanSelectedWord);
                    }
                } else if (e.getButton() == MouseButton.SECONDARY && info != null) {
                    showWordTooltip(t, info);
                }
            });
            flow.getChildren().add(t);
        }
        textVBox.getChildren().add(flow);
        pageLabel.setText("Page " + currentPage + "/" + totalPages);
    }

    private String cleanWordForSelection(String word) {
        // Remove leading/trailing non-alphanumeric characters (punctuation, symbols)
        // Keeps internal hyphens/apostrophes (e.g. "don't", "self-made")
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
            // Si está en la primera página, volver al home
            try {
                HomeController homeController = new HomeController();
                homeController.initialize();

                javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
                javafx.fxml.FXMLLoader sideMenuLoader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/leelo/side_menu.fxml"));
                javafx.scene.layout.VBox sideMenu = sideMenuLoader.load();

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
                FXMLLoader sideMenuLoader = new FXMLLoader(
                        getClass().getResource("/com/leelo/side_menu.fxml"));
                javafx.scene.layout.VBox sideMenu = sideMenuLoader.load();

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

        // get the last saved page
        int idSelectedText = selected.getIdText();
        currentPage = textService.getPage(idSelectedText);
        if (currentPage == 0) {
            currentPage = 1;
        }

        // Actualizar o crear el progreso para marcar este libro como el último leído
        textService.updateProgress(currentText.getIdText(), currentPage);

        totalPages = pages.size();
        showPage();
    }

    private void preparePages() {
        pages.clear();
        if (currentText == null)
            return;
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
                controller.termField.setText(word);
            }
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add/Edit");
            dialog.setScene(new Scene(root, 200, 250));
            dialog.showAndWait();
            // On close, refresh words and highlighting
            loadSavedWords();
            showPage();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String normalizeWord(String word) {
        // Remove punctuation, convert to lowercase and remove accents
        String withoutPunctuation = word.replaceAll("[^\\p{L}]", "").toLowerCase();
        String normalized = Normalizer.normalize(withoutPunctuation, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized;
    }

    // Method to show the state name
    private String stateToString(int state) {
        switch (state) {
            case 1:
                return "New";
            case 2:
                return "Learning";
            case 3:
                return "Learned";
            case 4:
                return "Mastered";
            default:
                return "Unknown";
        }
    }

    // Method to show a small tooltip below the word - Right click
    private void showWordTooltip(Text t, Word info) {

        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox box = new VBox(4);
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 10;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, 3);" +
                        "-fx-font-size: 20px;");

        Label trans = new Label(info.getTranslation() != null ? info.getTranslation() : "-");

        box.getChildren().addAll(trans);
        popup.getContent().add(box);

        Point2D p = t.localToScreen(0, t.getBoundsInLocal().getHeight() + 5);
        popup.show(t, p.getX(), p.getY());

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }

    // Method to automatically save a new word as Learning
    private void saveWordAsLearning(String wordNorm, String wordOriginal) {
        Word newWord = new Word();
        newWord.setTerm(wordOriginal);
        newWord.setTranslation("");
        newWord.setPronunciation("");
        newWord.setState(4);
        newWord.setUrlImg("");
        new com.leelo.service.WordService().addWord(newWord);
        loadSavedWords();
        openWordPopup(wordOriginal);
        showPage();
    }
}
