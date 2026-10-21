package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Language;
import com.leelo.model.LanguageDictionary;
import com.leelo.parse.ParserRegistry;
import com.leelo.service.LanguageService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The form used to create a language or to change the settings of one.
 */
public class languageFormController {

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> parserCombo;
    @FXML
    private CheckBox rightToLeftCheck;
    @FXML
    private CheckBox romanizationCheck;
    @FXML
    private TextField wordCharactersField;
    @FXML
    private TextField splitSentencesField;
    @FXML
    private TextField splitExceptionsField;
    @FXML
    private TextField characterSubstitutionsField;
    @FXML
    private TextArea dictionariesArea;
    @FXML
    private Label nameValidationLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private VBox messageContainer;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;

    private final LanguageService languageService = new LanguageService();
    private Language language;

    @FXML
    public void initialize() {
        Map<String, String> parsers = ParserRegistry.getSupportedNames();
        parserCombo.getItems().setAll(parsers.values());
        parserCombo.setValue(parsers.get(defaultParserType()));

        saveButton.setOnAction(event -> save());
        backButton.setOnAction(event -> goToLanguages());
    }

    /** Returns the key of the default parser. */
    private String defaultParserType() {
        return com.leelo.parse.SpaceDelimitedParser.TYPE;
    }

    /** Fills the form with the data of the language being edited. */
    public void setLanguage(Language selected) {
        this.language = selected;
        if (selected == null) {
            formTitleLabel.setText("Nuevo idioma");
            wordCharactersField.setText(Language.DEFAULT_WORD_CHARACTERS);
            splitSentencesField.setText(Language.DEFAULT_SPLIT_SENTENCES);
            splitExceptionsField.setText(Language.DEFAULT_SPLIT_EXCEPTIONS);
            characterSubstitutionsField.setText(Language.DEFAULT_CHARACTER_SUBSTITUTIONS);
            return;
        }
        formTitleLabel.setText("Editar idioma");
        nameField.setText(selected.getName());
        parserCombo.setValue(ParserRegistry.getSupportedNames().get(selected.getParserType()));
        rightToLeftCheck.setSelected(selected.isRightToLeft());
        romanizationCheck.setSelected(selected.isShowRomanization());
        wordCharactersField.setText(selected.getWordCharacters());
        splitSentencesField.setText(selected.getRegexpSplitSentences());
        splitExceptionsField.setText(selected.getExceptionsSplitSentences());
        characterSubstitutionsField.setText(selected.getCharacterSubstitutions());
        dictionariesArea.setText(formatDictionaries(selected.getDictionaries()));
    }

    /** Saves the language. */
    private void save() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            nameValidationLabel.setText("El nombre es obligatorio");
            nameValidationLabel.setVisible(true);
            return;
        }
        nameValidationLabel.setVisible(false);

        Language value = language != null ? language : new Language();
        value.setName(name);
        value.setParserType(parserTypeFor(parserCombo.getValue()));
        value.setRightToLeft(rightToLeftCheck.isSelected());
        value.setShowRomanization(romanizationCheck.isSelected());
        value.setWordCharacters(wordCharactersField.getText());
        value.setRegexpSplitSentences(splitSentencesField.getText());
        value.setExceptionsSplitSentences(splitExceptionsField.getText());
        value.setCharacterSubstitutions(characterSubstitutionsField.getText());
        value.setDictionaries(parseDictionaries(dictionariesArea.getText()));

        String error = languageService.validate(value);
        if (error != null) {
            showMessage(error);
            return;
        }
        languageService.save(value);
        goToLanguages();
    }

    /** Returns the parser type matching the selected display name. */
    private String parserTypeFor(String displayName) {
        for (Map.Entry<String, String> entry : ParserRegistry.getSupportedNames().entrySet()) {
            if (entry.getValue().equals(displayName)) {
                return entry.getKey();
            }
        }
        return com.leelo.parse.SpaceDelimitedParser.TYPE;
    }

    /** Reads the dictionaries from the text area. */
    private List<LanguageDictionary> parseDictionaries(String text) {
        List<LanguageDictionary> dictionaries = new ArrayList<>();
        if (text == null) {
            return dictionaries;
        }
        int order = 0;
        for (String line : text.split("\r?\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length < 3) {
                continue;
            }
            LanguageDictionary dictionary = new LanguageDictionary();
            dictionary.setUseFor(LanguageDictionary.UseFor.fromCode(parts[0].trim()));
            dictionary.setDictType(LanguageDictionary.DictType.fromCode(parts[1].trim()));
            dictionary.setDictUri(parts[2].trim());
            dictionary.setActive(true);
            dictionary.setSortOrder(order++);
            dictionaries.add(dictionary);
        }
        return dictionaries;
    }

    /** Writes the dictionaries into the text area. */
    private String formatDictionaries(List<LanguageDictionary> dictionaries) {
        StringBuilder builder = new StringBuilder();
        for (LanguageDictionary dictionary : dictionaries) {
            builder.append(dictionary.getUseFor().getCode())
                    .append(" | ")
                    .append(dictionary.getDictType().getCode())
                    .append(" | ")
                    .append(dictionary.getDictUri())
                    .append('\n');
        }
        return builder.toString();
    }

    /** Shows a message below the form. */
    private void showMessage(String message) {
        messageLabel.setText(message);
        messageContainer.setVisible(true);
        messageLabel.getStyleClass().removeAll("text-success", "text-error");
        messageLabel.getStyleClass().add("text-error");
    }

    /** Returns to the list of languages. */
    private void goToLanguages() {
        try {
            App.setRoot("languages");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
