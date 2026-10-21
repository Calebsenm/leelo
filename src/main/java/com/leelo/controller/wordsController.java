package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.service.ImportExportService;
import com.leelo.service.LanguageService;
import com.leelo.service.TermService;
import com.leelo.util.TextTokens;
import com.leelo.view.StatusConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * The vocabulary: the terms the user knows, or is learning.
 */
public class wordsController {

    @FXML
    private TableView<Term> wordsTable;
    @FXML
    private TableColumn<Term, String> termCol;
    @FXML
    private TableColumn<Term, String> translationCol;
    @FXML
    private TableColumn<Term, String> languageCol;
    @FXML
    private TableColumn<Term, String> statusCol;
    @FXML
    private TableColumn<Term, String> tagsCol;
    @FXML
    private TableColumn<Term, String> parentsCol;
    @FXML
    private TextField searchWord;
    @FXML
    private ComboBox<Language> languageCombo;
    @FXML
    private ComboBox<TermStatus> bulkStatusCombo;
    @FXML
    private CheckBox parentsOnlyCheck;
    @FXML
    private CheckBox includeIgnoredCheck;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button backButton;
    @FXML
    private Button bulkStatusButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button importButton;

    private final TermService termService = new TermService();
    private final LanguageService languageService = new LanguageService();
    private final ImportExportService importExportService = new ImportExportService();

    private final ObservableList<Term> wordsList = FXCollections.observableArrayList();
    private FilteredList<Term> filteredWords;

    @FXML
    public void initialize() {
        termCol.setCellValueFactory(new PropertyValueFactory<>("displayText"));
        translationCol.setCellValueFactory(new PropertyValueFactory<>("translation"));
        languageCol.setCellValueFactory(new PropertyValueFactory<>("languageName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusLabel"));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                int index = getIndex();
                if (empty || getTableView() == null || index < 0 || index >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Term term = getTableView().getItems().get(index);
                Label pill = new Label(StatusConverter.labelFor(term.getStatus()));
                pill.getStyleClass().addAll("status-pill", "status-pill-" + term.getStatus().getCode());
                setGraphic(pill);
                setText(null);
            }
        });
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tagList"));
        parentsCol.setCellValueFactory(new PropertyValueFactory<>("parentList"));

        setupFilters();
        loadWords();

        addButton.setOnAction(event -> goToAddWord());
        editButton.setOnAction(event -> editSelectedWord());
        deleteButton.setOnAction(event -> deleteSelectedWord());
        backButton.setOnAction(event -> goToHome());
        bulkStatusButton.setOnAction(event -> applyBulkStatus());
        exportButton.setOnAction(event -> exportToCsv());
        importButton.setOnAction(event -> importFromCsv());
    }

    /** Fills the filter controls. */
    private void setupFilters() {
        List<Language> languages = languageService.getSupported();
        languageCombo.getItems().setAll(languages);
        languageCombo.setPromptText("Todos los idiomas");
        languageCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        List<TermStatus> statuses = new ArrayList<>();
        for (TermStatus status : TermStatus.values()) {
            if (status.isAssignable()) {
                statuses.add(status);
            }
        }
        bulkStatusCombo.getItems().setAll(statuses);
        bulkStatusCombo.setConverter(new com.leelo.view.StatusConverter());
        bulkStatusCombo.setValue(TermStatus.WELL_KNOWN);

        searchWord.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        parentsOnlyCheck.selectedProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        includeIgnoredCheck.selectedProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    /** Loads the terms of the vocabulary. */
    private void loadWords() {
        wordsList.setAll(termService.getAll(0));
        filteredWords = new FilteredList<>(wordsList, term -> true);
        wordsTable.setItems(filteredWords);
    }

    /** Applies the filters of the list. */
    private void applyFilters() {
        if (filteredWords == null) {
            return;
        }
        String search = searchWord.getText() == null ? "" : searchWord.getText().trim().toLowerCase();
        Language language = languageCombo.getValue();
        boolean parentsOnly = parentsOnlyCheck.isSelected();
        boolean includeIgnored = includeIgnoredCheck.isSelected();

        filteredWords.setPredicate(term -> {
            if (!search.isEmpty()) {
                boolean matches = TextTokens.strip(term.getText()).toLowerCase().contains(search)
                        || term.getTranslation().toLowerCase().contains(search);
                if (!matches) {
                    return false;
                }
            }
            if (language != null && term.getLanguageId() != language.getId()) {
                return false;
            }
            if (parentsOnly && !term.getParents().isEmpty()) {
                return false;
            }
            if (!includeIgnored && term.getStatus() == TermStatus.IGNORED) {
                return false;
            }
            return true;
        });
    }

    /** Opens the form used to add a term. */
    private void goToAddWord() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    App.class.getResource("add_word.fxml"));
            javafx.scene.Parent root = loader.load();
            addWordController controller = loader.getController();
            controller.setDefaultTerm(null);

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("Agregar palabra");
            dialog.setScene(new javafx.scene.Scene(root, 480, 720));
            dialog.showAndWait();
            loadWords();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Opens the form used to edit the selected term. */
    private void editSelectedWord() {
        Term selected = wordsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    App.class.getResource("add_word.fxml"));
            javafx.scene.Parent root = loader.load();
            addWordController controller = loader.getController();
            controller.setWordToEdit(selected);

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("Editar palabra");
            dialog.setScene(new javafx.scene.Scene(root, 480, 720));
            dialog.showAndWait();
            loadWords();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Deletes the selected term. */
    private void deleteSelectedWord() {
        Term selected = wordsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        termService.delete(selected.getId());
        loadWords();
    }

    /** Applies the selected status to every selected term. */
    private void applyBulkStatus() {
        TermStatus status = bulkStatusCombo.getValue();
        if (status == null) {
            return;
        }
        List<Term> selected = wordsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Term term : selected) {
            ids.add(term.getId());
        }
        termService.setStatusForAll(ids, status);
        loadWords();
    }

    /** Writes the terms that pass the filters to a CSV file. */
    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar palabras");
        fileChooser.setInitialFileName("Terms.csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        if (file == null) {
            return;
        }
        try (Writer writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            importExportService.exportTerms(new ArrayList<>(filteredWords), writer);
        } catch (Exception e) {
            showError("No se pudo exportar: " + e.getMessage());
        }
    }

    /** Reads the terms from a CSV file. */
    private void importFromCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar palabras");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
        File file = fileChooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        try {
            ImportExportService.ImportOptions options = new ImportExportService.ImportOptions(true, true, false);
            ImportExportService.ImportResult result;
            try (var input = java.nio.file.Files.newInputStream(file.toPath())) {
                result = importExportService.importTerms(input, options);
            }
            loadWords();
            Alert alert = new Alert(result.errors().isEmpty() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            alert.setTitle("Importacion de palabras");
            alert.setHeaderText(result.created() + " creadas, " + result.updated() + " actualizadas");
            alert.setContentText(result.errors().isEmpty()
                    ? "La importacion termino correctamente."
                    : String.join("\n", result.errors()));
            alert.showAndWait();
        } catch (Exception e) {
            showError("No se pudo importar: " + e.getMessage());
        }
    }

    /** Returns to the home screen. */
    private void goToHome() {
        try {
            App.showHome();
        } catch (Exception e) {
            e.printStackTrace();
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
