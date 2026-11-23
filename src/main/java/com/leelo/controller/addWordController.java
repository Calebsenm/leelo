package com.leelo.controller;

import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.model.AiDefinition;
import com.leelo.service.LanguageService;
import com.leelo.service.OllamaService;
import com.leelo.service.TermService;
import com.leelo.util.TextTokens;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * The form used to create a term or to change an existing one.
 */
public class addWordController {

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Language> languageCombo;
    @FXML
    private TextField termField;
    @FXML
    private TextArea translationArea;
    @FXML
    private Button aiTranslateButton;
    @FXML
    private Label aiStatusLabel;
    @FXML
    private TextField pronunciationField;
    @FXML
    private ComboBox<TermStatus> statusCombo;
    @FXML
    private TextField parentsField;
    @FXML
    private TextField tagsField;
    @FXML
    private TextField urlImgField;
    @FXML
    private ImageView imagePreview;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label messageLabel;

    private final TermService termService = new TermService();
    private final LanguageService languageService = new LanguageService();
    private final OllamaService ollamaService = new OllamaService();

    private Term wordToEdit;

    @FXML
    public void initialize() {
        List<Language> languages = languageService.getSupported();
        languageCombo.getItems().setAll(languages);
        if (!languages.isEmpty()) {
            languageCombo.setValue(languages.get(0));
        }

        List<TermStatus> statuses = new ArrayList<>();
        for (TermStatus status : TermStatus.values()) {
            if (status.isAssignable()) {
                statuses.add(status);
            }
        }
        statusCombo.getItems().setAll(statuses);
        statusCombo.setConverter(new com.leelo.view.StatusConverter());
        statusCombo.setValue(TermStatus.NEW_1);

        saveButton.setOnAction(event -> saveOrUpdateWord());
        deleteButton.setOnAction(event -> deleteWord());
        cancelButton.setOnAction(event -> closeWindow());
        aiTranslateButton.setOnAction(event -> translateWithAi());
        setDeleteVisible(false);
        urlImgField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview(newValue));
    }

    /** Fills the form with the text of a new term. */
    public void setDefaultTerm(String term) {
        titleLabel.setText("Nueva palabra");
        setDeleteVisible(false);
        if (wordToEdit == null && term != null && !term.trim().isEmpty()) {
            termField.setText(term.trim());
        }
    }

    /** Shows the delete action only while editing an existing term. */
    private void setDeleteVisible(boolean visible) {
        deleteButton.setVisible(visible);
        deleteButton.setManaged(visible);
    }

    /** Fills the form with the data of the term being edited. */
    public void setWordToEdit(Term term) {
        this.wordToEdit = term;
        if (term == null) {
            titleLabel.setText("Nueva palabra");
            setDeleteVisible(false);
            urlImgField.clear();
            refreshPreview("");
            return;
        }
        titleLabel.setText("Editar palabra");
        setDeleteVisible(true);
        termField.setText(TextTokens.strip(term.getText()));
        translationArea.setText(term.getTranslation());
        pronunciationField.setText(term.getRomanization());
        statusCombo.setValue(term.getStatus());
        parentsField.setText(term.getParentList());
        tagsField.setText(term.getTagList());
        languageCombo.setValue(languageService.getById(term.getLanguageId()));
        String source = term.getImage() == null ? "" : term.getImage().getSource();
        urlImgField.setText(source);
        refreshPreview(source);
    }

    /** Saves the term. */
    private void saveOrUpdateWord() {
        String text = termField.getText() == null ? "" : termField.getText().trim();
        if (text.isEmpty()) {
            showMessage("El termino es obligatorio.", true);
            return;
        }
        Language language = languageCombo.getValue();
        if (language == null) {
            showMessage("Selecciona un idioma.", true);
            return;
        }

        Term term = wordToEdit != null ? wordToEdit : termService.buildTerm(language, text);
        if (wordToEdit != null) {
            term.setText(termService.parseText(language, text));
            term.setTextLc(termService.lowercase(language, term.getText()));
            term.setTokenCount(TextTokens.countTokens(term.getText()));
        }
        term.setLanguageId(language.getId());
        term.setTranslation(translationArea.getText());
        term.setRomanization(pronunciationField.getText());
        TermStatus status = statusCombo.getValue();
        if (status != null) {
            term.setStatus(status);
        }
        term.setTags(termService.resolveTags(splitList(tagsField.getText())));
        term.setParents(resolveParents(language, parentsField.getText()));

        String error = termService.validate(term);
        if (error != null) {
            showMessage(error, true);
            return;
        }
        termService.save(term);
        termService.setImage(term.getId(), urlImgField.getText() == null ? "" : urlImgField.getText().trim());
        closeWindow();
    }

    /** Deletes the term being edited. */
    private void deleteWord() {
        if (wordToEdit == null) {
            return;
        }
        termService.delete(wordToEdit.getId());
        closeWindow();
    }

    /** Turns the parent names into the terms they point at. */
    private List<Term> resolveParents(Language language, String text) {
        List<Term> parents = new ArrayList<>();
        for (String name : splitList(text)) {
            Term parent = termService.findOrCreate(language, name);
            if (parent.getId() > 0 && (wordToEdit == null || parent.getId() != wordToEdit.getId())) {
                parents.add(parent);
            }
        }
        return parents;
    }

    /** Splits a comma separated list. */
    private List<String> splitList(String text) {
        List<String> items = new ArrayList<>();
        if (text == null) {
            return items;
        }
        for (String item : text.split(",")) {
            if (!item.isBlank()) {
                items.add(item.trim());
            }
        }
        return items;
    }

    /** Shows a message below the form. */
    private void showMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.getStyleClass().removeAll("text-error", "text-success");
        messageLabel.getStyleClass().add(error ? "text-error" : "text-success");
    }

    /** Loads the preview of the image of the term. */
    private void refreshPreview(String imageUrl) {
        String cleanUrl = imageUrl == null ? "" : imageUrl.trim();
        if (cleanUrl.isEmpty()) {
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            imagePreview.setManaged(false);
            return;
        }
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() {
                return new Image(cleanUrl, true);
            }
        };
        task.setOnSucceeded(event -> {
            imagePreview.setImage(task.getValue());
            imagePreview.setVisible(true);
            imagePreview.setManaged(true);
        });
        task.setOnFailed(event -> {
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            imagePreview.setManaged(false);
        });
        Thread worker = new Thread(task, "leelo-term-image");
        worker.setDaemon(true);
        worker.start();
    }

    /** Queries Ollama to generate a rich bilingual definition for the term in termField. */
    private void translateWithAi() {
        String text = termField.getText() == null ? "" : termField.getText().trim();
        if (text.isEmpty()) {
            showMessage("Escribe un término antes de consultar la IA.", true);
            return;
        }

        aiTranslateButton.setDisable(true);
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("Consultando Ollama...");
            aiStatusLabel.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 11px;");
            aiStatusLabel.setVisible(true);
            aiStatusLabel.setManaged(true);
        }

        Task<AiDefinition> task = new Task<>() {
            @Override
            protected AiDefinition call() throws Exception {
                return ollamaService.generateDefinition(text, "");
            }
        };

        task.setOnSucceeded(event -> {
            aiTranslateButton.setDisable(false);
            AiDefinition def = task.getValue();
            if (def != null) {
                translationArea.setText(def.formatForTranslationArea());
                if (aiStatusLabel != null) {
                    aiStatusLabel.setText("✓ Traducido con Ollama");
                    aiStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
                }
            }
        });

        task.setOnFailed(event -> {
            aiTranslateButton.setDisable(false);
            Throwable err = task.getException();
            String msg = err != null && err.getMessage() != null ? err.getMessage() : "Error al consultar Ollama";
            if (aiStatusLabel != null) {
                aiStatusLabel.setText(msg);
                aiStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            }
        });

        Thread thread = new Thread(task, "leelo-addword-ollama");
        thread.setDaemon(true);
        thread.start();
    }

    /** Closes the window holding the form. */
    private void closeWindow() {
        javafx.stage.Window window = saveButton.getScene().getWindow();
        if (window != null) {
            window.hide();
        }
    }
}
