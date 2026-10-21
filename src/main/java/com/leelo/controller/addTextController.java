package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Book;
import com.leelo.model.Language;
import com.leelo.service.BookService;
import com.leelo.service.LanguageService;
import com.leelo.service.PageSplitter;
import com.leelo.service.SettingService;
import com.leelo.service.TextImportService;
import com.leelo.util.TextCleaner;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The screen used to add a book or to change its metadata.
 */
public class addTextController {

    @FXML
    private TextField titleField;
    @FXML
    private TextField tagsField;
    @FXML
    private TextField sourceUriField;
    @FXML
    private TextArea areaContent;
    @FXML
    private ComboBox<Language> languageCombo;
    @FXML
    private ComboBox<PageSplitter.SplitBy> splitByCombo;
    @FXML
    private Spinner<Integer> thresholdSpinner;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;
    @FXML
    private Button pdfButton;
    @FXML
    private Button txtButton;
    @FXML
    private Button urlButton;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label titleValidationLabel;
    @FXML
    private Label contentValidationLabel;
    @FXML
    private Label languageValidationLabel;
    @FXML
    private Label characterCountLabel;
    @FXML
    private Label wordCountLabel;
    @FXML
    private VBox messageContainer;
    @FXML
    private HBox buttonContainer;

    private final BookService bookService = new BookService();
    private final LanguageService languageService = new LanguageService();
    private final SettingService settingService = new SettingService();
    private final TextImportService textImportService = new TextImportService();

    private Book bookToEdit;
    private boolean isLoading;

    @FXML
    public void initialize() {
        setupLanguages();
        splitByCombo.getItems().setAll(PageSplitter.SplitBy.values());
        splitByCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(PageSplitter.SplitBy value) {
                return value == null ? "" : switch (value) {
                    case PARAGRAPHS -> "Párrafos";
                    case SENTENCES -> "Frases";
                };
            }

            @Override
            public PageSplitter.SplitBy fromString(String value) {
                return PageSplitter.SplitBy.PARAGRAPHS;
            }
        });
        splitByCombo.setValue(PageSplitter.SplitBy.PARAGRAPHS);
        thresholdSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1500, PageSplitter.DEFAULT_THRESHOLD, 10));

        saveButton.setOnAction(event -> saveOrUpdateText());
        backButton.setOnAction(event -> goToTexts());
        pdfButton.setOnAction(event -> loadFromPdf());
        txtButton.setOnAction(event -> loadFromTxt());
        urlButton.setOnAction(event -> loadFromUrl());

        areaContent.textProperty().addListener((observable, oldValue, newValue) -> {
            updateCharacterCount();
            clearContentValidation();
        });
        titleField.textProperty().addListener((observable, oldValue, newValue) -> clearTitleValidation());
    }

    /** Fills the combo box with the available languages. */
    private void setupLanguages() {
        List<Language> languages = languageService.getSupported();
        languageCombo.getItems().setAll(languages);
        int currentLanguageId = settingService.getCurrentLanguageId();
        languages.stream()
                .filter(language -> language.getId() == currentLanguageId)
                .findFirst()
                .ifPresent(languageCombo::setValue);
        if (languageCombo.getValue() == null && !languages.isEmpty()) {
            languageCombo.setValue(languages.get(0));
        }
    }

    /** Fills the form with the data of a book that is being edited. */
    public void setBookToEdit(Book book) {
        this.bookToEdit = book;
        if (book == null) {
            return;
        }
        formTitleLabel.setText("Editar Texto");
        saveButton.setText("Actualizar");
        titleField.setText(book.getTitle());
        sourceUriField.setText(book.getSourceUri());
        tagsField.setText(book.getTagList());
        languageCombo.setValue(languageService.getById(book.getLanguageId()));
        areaContent.setText(bookService.getPages(book.getId()).stream()
                .map(page -> page.getContent())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse(""));
        updateCharacterCount();
    }

    /** Counts the characters and the words of the content. */
    private void updateCharacterCount() {
        String content = areaContent.getText();
        int wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        characterCountLabel.setText(content.length() + " caracteres");
        wordCountLabel.setText(wordCount + " palabras");
    }

    /** Saves the book. */
    private void saveOrUpdateText() {
        if (isLoading) {
            return;
        }
        if (!validate()) {
            showMessage("Revisa los campos marcados en rojo.", MessageType.ERROR);
            return;
        }
        setLoadingState(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                Language language = languageCombo.getValue();
                List<String> tags = splitTags(tagsField.getText());
                String source = sourceUriField.getText() == null ? "" : sourceUriField.getText().trim();
                int threshold = thresholdSpinner.getValue() == null
                        ? PageSplitter.DEFAULT_THRESHOLD : thresholdSpinner.getValue();
                PageSplitter.SplitBy splitBy = splitByCombo.getValue() == null
                        ? PageSplitter.SplitBy.PARAGRAPHS : splitByCombo.getValue();

                if (bookToEdit != null) {
                    bookToEdit.setTitle(titleField.getText().trim());
                    bookToEdit.setSourceUri(source);
                    bookToEdit.setLanguageId(language.getId());
                    return bookService.update(bookToEdit, tags);
                }
                return bookService.create(titleField.getText().trim(), language, areaContent.getText(),
                        source, splitBy, threshold, tags) > 0;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            setLoadingState(false);
            if (Boolean.TRUE.equals(task.getValue())) {
                showMessage(bookToEdit != null ? "Texto actualizado." : "Texto guardado.",
                        MessageType.SUCCESS);
                PauseTransition delay = new PauseTransition(Duration.seconds(1));
                delay.setOnFinished(event2 -> goToTexts());
                delay.play();
            } else {
                showMessage("No se pudo guardar el texto.", MessageType.ERROR);
            }
        }));
        task.setOnFailed(event -> Platform.runLater(() -> {
            setLoadingState(false);
            showMessage("Ocurrio un error al guardar.", MessageType.ERROR);
        }));

        Thread worker = new Thread(task, "leelo-save-text");
        worker.setDaemon(true);
        worker.start();
    }

    /** Validates the form. */
    private boolean validate() {
        boolean valid = true;
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            showValidation(titleValidationLabel, "El titulo es obligatorio", true);
            valid = false;
        } else {
            showValidation(titleValidationLabel, "", false);
        }

        String content = areaContent.getText() == null ? "" : areaContent.getText().trim();
        if (content.isEmpty()) {
            showValidation(contentValidationLabel, "El contenido es obligatorio", true);
            valid = false;
        } else {
            showValidation(contentValidationLabel, "", false);
        }

        if (languageCombo.getValue() == null) {
            showValidation(languageValidationLabel, "Selecciona un idioma", true);
            valid = false;
        } else {
            showValidation(languageValidationLabel, "", false);
        }
        return valid;
    }

    /** Shows or hides a validation message. */
    private void showValidation(Label label, String message, boolean error) {
        label.setText(message);
        label.setVisible(!message.isEmpty());
        label.getStyleClass().removeAll("text-success");
        label.getStyleClass().add(error ? "text-error" : "text-success");
    }

    private void clearTitleValidation() {
        showValidation(titleValidationLabel, "", false);
    }

    private void clearContentValidation() {
        showValidation(contentValidationLabel, "", false);
    }

    /** Loads the content of a PDF file. */
    private void loadFromPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona un archivo PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        File selected = fileChooser.showOpenDialog(null);
        if (selected == null) {
            return;
        }
        try {
            String content = textImportService.fromPdfFile(selected);
            areaContent.setText(content);
            titleField.setText(stripExtension(selected.getName()));
            updateCharacterCount();
        } catch (Exception e) {
            showError("No se pudo leer el PDF: " + e.getMessage());
        }
    }

    /** Loads the content of a plain text file. */
    private void loadFromTxt() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona un archivo de texto");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de texto (*.txt)", "*.txt"));
        File selected = fileChooser.showOpenDialog(null);
        if (selected == null) {
            return;
        }
        try {
            String content = TextCleaner.clean(Files.readString(selected.toPath()));
            areaContent.setText(content);
            titleField.setText(stripExtension(selected.getName()));
            updateCharacterCount();
        } catch (Exception e) {
            showError("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    /** Loads the content of a web page. */
    private void loadFromUrl() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/leelo/add_text_url.fxml"));
            javafx.scene.Parent root = loader.load();
            addUrlController controller = loader.getController();
            controller.setParentController(this);

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("Agregar URL");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.showAndWait();
        } catch (Exception e) {
            showError("No se pudo abrir la ventana: " + e.getMessage());
        }
    }

    /** Fills the form with the content of a web page. */
    public void addTextFromUrl(String title, String content) {
        titleField.setText(title);
        areaContent.setText(TextCleaner.clean(content));
        updateCharacterCount();
    }

    /** Removes the extension of a file name. */
    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Splits a comma separated list of tags. */
    private List<String> splitTags(String text) {
        List<String> tags = new ArrayList<>();
        if (text == null) {
            return tags;
        }
        for (String tag : text.split(",")) {
            if (!tag.isBlank()) {
                tags.add(tag.trim());
            }
        }
        return tags;
    }

    /** Shows or hides the loading state of the form. */
    private void setLoadingState(boolean loading) {
        isLoading = loading;
        buttonContainer.setDisable(loading);
        titleField.setDisable(loading);
        areaContent.setDisable(loading);
    }

    /** Returns to the library. */
    private void goToTexts() {
        try {
            App.setRoot("texts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** The kind of message shown below the form. */
    private enum MessageType {
        SUCCESS, ERROR, INFO
    }

    /** Shows a message below the form. */
    private void showMessage(String message, MessageType type) {
        messageLabel.setText(message);
        messageContainer.setVisible(true);
        messageLabel.getStyleClass().removeAll("text-success", "text-error", "text-secondary");
        switch (type) {
            case SUCCESS -> messageLabel.getStyleClass().add("text-success");
            case ERROR -> messageLabel.getStyleClass().add("text-error");
            case INFO -> messageLabel.getStyleClass().add("text-secondary");
        }
    }

    /** Shows an error dialog. */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
