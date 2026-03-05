package com.leelo.controller;

import com.leelo.model.Word;
import com.leelo.service.WordService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class addWordController {
    @FXML public TextField termField;
    @FXML private Label titleLabel;
    @FXML private TextField pronunciationField;
    @FXML private ComboBox<String> stateCombo;
    @FXML private TextField urlImgField;
    @FXML private Button saveButton;
    @FXML private Label messageLabel;
    @FXML private VBox meaningsContainer;
    @FXML private Button addMeaningButton;

    private final WordService WordService = new WordService();
    private Word wordToEdit = null;

    @FXML
    public void initialize() {
        stateCombo.setItems(FXCollections.observableArrayList("New", "Learning", "Learned", "Mastered"));
        stateCombo.setValue("Learning");
        addMeaningButton.setOnAction(e -> addMeaningField(""));
        saveButton.setOnAction(e -> saveOrUpdateWord());
        ensureAtLeastOneMeaningField();
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
