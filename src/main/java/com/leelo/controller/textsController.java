package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Book;
import com.leelo.service.BookService;
import com.leelo.service.SettingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * The library: the books the user reads.
 */
public class textsController {

    @FXML
    private TableView<Book> textsTable;
    @FXML
    private TableColumn<Book, String> titleCol;
    @FXML
    private TableColumn<Book, String> languageCol;
    @FXML
    private TableColumn<Book, Integer> pagesCol;
    @FXML
    private TableColumn<Book, Integer> progressCol;
    @FXML
    private TableColumn<Book, String> dateCol;
    @FXML
    private TextField searchText;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button readButton;
    @FXML
    private Button archiveButton;
    @FXML
    private Button backButton;

    private final BookService bookService = new BookService();
    private final SettingService settingService = new SettingService();
    private final ObservableList<Book> booksList = FXCollections.observableArrayList();
    private FilteredList<Book> filteredBooks;

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        languageCol.setCellValueFactory(new PropertyValueFactory<>("languageName"));
        pagesCol.setCellValueFactory(new PropertyValueFactory<>("pageCount"));
        progressCol.setCellValueFactory(new PropertyValueFactory<>("progressPercent"));
        progressCol.setCellFactory(column -> new TableCell<Book, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                int index = getIndex();
                if (empty || getTableView() == null || index < 0 || index >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Book book = getTableView().getItems().get(index);
                ProgressBar bar = new ProgressBar(book.getProgressPercent() / 100.0);
                bar.setPrefWidth(72);
                bar.getStyleClass().add("progress-bar");
                Label percent = new Label(book.getProgressPercent() + "%");
                percent.getStyleClass().add("text-xs");
                percent.setMinWidth(34);
                percent.setAlignment(Pos.CENTER_RIGHT);
                HBox box = new HBox(8, bar, percent);
                box.getStyleClass().add("table-progress");
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        dateCol.setCellValueFactory(new PropertyValueFactory<>("creationDate"));

        loadBooks();
        setupSearch();

        addButton.setOnAction(event -> goToAddText());
        editButton.setOnAction(event -> editSelectedText());
        deleteButton.setOnAction(event -> deleteSelectedText());
        readButton.setOnAction(event -> readSelectedText());
        archiveButton.setOnAction(event -> archiveSelectedText());
        backButton.setOnAction(event -> goToHome());

        textsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                readSelectedText();
            }
        });
    }

    /** Loads the books of the library. */
    private void loadBooks() {
        List<Book> books = bookService.getAll(settingService.getCurrentLanguageId(), false);
        booksList.setAll(books);
        filteredBooks = new FilteredList<>(booksList, book -> true);
        textsTable.setItems(filteredBooks);
    }

    /** Filters the books as the user types. */
    private void setupSearch() {
        searchText.textProperty().addListener((observable, oldValue, newValue) -> {
            String filter = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredBooks.setPredicate(book -> filter.isEmpty()
                    || book.getTitle().toLowerCase().contains(filter)
                    || book.getLanguageName().toLowerCase().contains(filter));
        });
    }

    /** Opens the screen used to add a book. */
    private void goToAddText() {
        try {
            App.setRoot("add_text");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Opens the screen used to edit the selected book. */
    private void editSelectedText() {
        Book selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    App.class.getResource("add_text.fxml"));
            javafx.scene.Parent root = loader.load();
            addTextController controller = loader.getController();
            controller.setBookToEdit(selected);
            App.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Opens the selected book in the reading screen. */
    private void readSelectedText() {
        Book selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            App.openReading(selected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Archives or unarchives the selected book. */
    private void archiveSelectedText() {
        Book selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        bookService.setArchived(selected.getId(), !selected.isArchived());
        loadBooks();
    }

    /** Deletes the selected book. */
    private void deleteSelectedText() {
        Book selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Eliminar texto");
        confirmation.setHeaderText("Se eliminara '" + selected.getTitle() + "'");
        confirmation.setContentText("Esta accion no se puede deshacer.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            bookService.delete(selected.getId());
            loadBooks();
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
