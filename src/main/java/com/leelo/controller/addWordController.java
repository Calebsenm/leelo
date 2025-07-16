package com.leelo.controller;

import com.leelo.model.Word;
import com.leelo.service.WordService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class addWordController {
    @FXML public TextField termField;
    @FXML private TextField translationField;
    @FXML private TextField pronunciationField;
    @FXML private ComboBox<String> stateCombo;
    @FXML private TextField urlImgField;
    @FXML private Button saveButton;
    @FXML private Label messageLabel;

    private WordService WordService = new WordService();
    private Word wordToEdit = null;

    public void setWordToEdit(Word word) {
        this.wordToEdit = word;
        if (word != null) {
            termField.setText(word.getTerm());
            translationField.setText(word.getTranslation());
            pronunciationField.setText(word.getPronunciation());
            stateCombo.setValue(stateToString(word.getState()));
            urlImgField.setText(word.getUrlImg());
            this.wordToEdit.setIdTerm(word.getIdTerm());
        }
    }

    @FXML
    public void initialize() {
        stateCombo.setItems(FXCollections.observableArrayList("New", "Learning", "Learned", "Mastered"));
        saveButton.setOnAction(e -> saveOrUpdateWord());
    }

    private void saveOrUpdateWord() {
        String term = termField.getText();
        String translation = translationField.getText();
        String pronunciation = pronunciationField.getText();
        String stateStr = stateCombo.getValue();
        String urlImg = urlImgField.getText();
        if (term.isEmpty() || stateStr == null || translation == null || translation.trim().isEmpty()) {
            showMessage("Complete all required fields.", true);
            return;
        }
        int state = stringToState(stateStr);
        // Si es palabra nueva y la traducción está vacía, forzar estado Mastered
        if (wordToEdit == null && (translation == null || translation.trim().isEmpty())) {
            state = 4;
            stateStr = "Mastered";
            stateCombo.setValue(stateStr);
        }
        if (state < 1 || state > 4) {
            state = 1;
            stateStr = "New";
            stateCombo.setValue(stateStr);
        }
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
            showMessage("Saved successfully!", false);
            // Close window if modal
            javafx.scene.Node node = saveButton;
            javafx.stage.Window window = node.getScene().getWindow();
            window.hide();
        } else {
            showMessage("Could not save the word.", true);
        }
    }


    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: red;");
        } else {
            messageLabel.setStyle("-fx-text-fill: green;");
        }
    }

    private int stringToState(String state) {
        switch (state) {
            case "New": return 1;
            case "Learning": return 2;
            case "Learned": return 3;
            case "Mastered": return 4;
            default: return 1;
        }
    }
    private String stateToString(int state) {
        switch (state) {
            case 1: return "New";
            case 2: return "Learning";
            case 3: return "Learned";
            case 4: return "Mastered";
            default: return "New";
        }
    }
} 