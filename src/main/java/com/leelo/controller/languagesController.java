package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Book;
import com.leelo.model.Language;
import com.leelo.parse.ParserRegistry;
import com.leelo.service.BookService;
import com.leelo.service.LanguageService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The list of the languages the user studies.
 */
public class languagesController {

    @FXML
    private TableView<Language> languagesTable;
    @FXML
    private TableColumn<Language, String> nameCol;
    @FXML
    private TableColumn<Language, String> parserCol;
    @FXML
    private TableColumn<Language, Integer> booksCol;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button backButton;

    private final LanguageService languageService = new LanguageService();
    private final BookService bookService = new BookService();
    private final ObservableList<Language> languages = FXCollections.observableArrayList();
    private final Map<Integer, Integer> bookCounts = new HashMap<>();

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        parserCol.setCellValueFactory(new PropertyValueFactory<>("parserName"));
        booksCol.setCellValueFactory(new PropertyValueFactory<>("bookCount"));

        loadLanguages();

        addButton.setOnAction(event -> openForm(null));
        editButton.setOnAction(event -> openForm(selectedLanguage()));
        deleteButton.setOnAction(event -> deleteLanguage());
        backButton.setOnAction(event -> goToHome());
    }

    /** Loads the languages and the number of books of each of them. */
    private void loadLanguages() {
        languages.clear();
        bookCounts.clear();

        for (Book book : bookService.getAll(0, true)) {
            bookCounts.merge(book.getLanguageId(), 1, Integer::sum);
        }
        Map<String, String> parserNames = ParserRegistry.getSupportedNames();
        for (Language language : languageService.getAll()) {
            language.setBookCount(bookCounts.getOrDefault(language.getId(), 0));
            language.setParserName(parserNames.getOrDefault(language.getParserType(), language.getParserType()));
            languages.add(language);
        }
        languagesTable.setItems(languages);
    }

    /** Returns the selected language, or {@code null}. */
    private Language selectedLanguage() {
        return languagesTable.getSelectionModel().getSelectedItem();
    }

    /** Opens the form used to create or to edit a language. */
    private void openForm(Language language) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    App.class.getResource("language_form.fxml"));
            javafx.scene.Parent root = loader.load();
            languageFormController controller = loader.getController();
            controller.setLanguage(language);
            App.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Deletes the selected language. */
    private void deleteLanguage() {
        Language language = selectedLanguage();
        if (language == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Eliminar idioma");
        confirmation.setHeaderText("Se eliminara '" + language.getName() + "'");
        confirmation.setContentText("Tambien se eliminaran sus textos y sus palabras.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            languageService.delete(language.getId());
            loadLanguages();
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
}
