package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Texts;
import com.leelo.service.TextService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class textsController {
    @FXML
    private TableView<Texts> textsTable;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button backButton;
    @FXML
    private Button readButton;
    @FXML
    private TextField searchText;
    @FXML
    private TableColumn<Texts, String> titleCol;
    @FXML
    private TableColumn<Texts, String> dateCol;

    private TextService TextService = new TextService();
    private ObservableList<Texts> textsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        titleCol.setCellValueFactory(new PropertyValueFactory<>("tittle"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("creationDate"));

        loadTexts();
        addButton.setOnAction(e -> goToAddText());
        editButton.setOnAction(e -> editSelectedText());
        deleteButton.setOnAction(e -> deleteSelectedText());
        backButton.setOnAction(e -> goToHome());
        readButton.setOnAction(e -> readSelectedText());

        textsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                readSelectedText();
            }
        });

        // search a text by name
        searchText.textProperty().addListener((observable, oldValue, newValue) -> {
            String filter = newValue.toLowerCase(); 
            List<Texts> filteredTexts = TextService.listAllTexts().stream()
                    .filter(text -> text.getTittle().toLowerCase().contains(filter))
                    .toList(); 

            textsList.setAll(filteredTexts);
        });

    }

    private void loadTexts() {
        List<Texts> texts = TextService.listAllTexts();
        textsList.setAll(texts);
        textsTable.setItems(textsList);
    }

    private void goToAddText() {
        try {
            App.setRoot("add_text");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editSelectedText() {
        Texts selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            // Optional: show selection required message
            return;
        }
        try {
        
            FXMLLoader loader = new FXMLLoader(App.class.getResource("add_text.fxml"));
            Parent root = loader.load();

            addTextController controller = loader.getController();
            controller.setTextToEdit(selected);
        
            App.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedText() {
        Texts selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            // Optional: show selection required message
            return;
        }
        boolean ok = TextService.deleteText(selected.getIdText());
        if (ok) {
            loadTexts();
        } else {
            // Optional: show error message
        }
    }

    private void goToHome() {
        try {
            App.setRoot("home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readSelectedText() {

        Texts selected = textsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {

            FXMLLoader loader = new FXMLLoader(App.class.getResource("reading.fxml"));
            Parent root = loader.load();

            ReadingController controller = loader.getController();
            controller.setText(selected);
            
            App.getScene().setRoot(root);

            Stage stage = (Stage) App.getScene().getWindow();
            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}