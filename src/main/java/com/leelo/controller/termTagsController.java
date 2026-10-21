package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.TermTag;
import com.leelo.service.TermTagService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

/**
 * The tags that group the terms of the vocabulary.
 */
public class termTagsController {

    @FXML
    private TableView<TermTag> tagsTable;
    @FXML
    private TableColumn<TermTag, String> textCol;
    @FXML
    private TableColumn<TermTag, String> commentCol;
    @FXML
    private TableColumn<TermTag, Integer> countCol;
    @FXML
    private TextField newTagField;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button backButton;

    private final TermTagService termTagService = new TermTagService();
    private final ObservableList<TermTag> tags = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        textCol.setCellValueFactory(new PropertyValueFactory<>("text"));
        commentCol.setCellValueFactory(new PropertyValueFactory<>("comment"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("termCount"));

        loadTags();

        addButton.setOnAction(event -> addTag());
        editButton.setOnAction(event -> editTag());
        deleteButton.setOnAction(event -> deleteTag());
        backButton.setOnAction(event -> goToHome());
        newTagField.setOnAction(event -> addTag());
    }

    /** Loads the tags. */
    private void loadTags() {
        tags.setAll(termTagService.getAll());
        tagsTable.setItems(tags);
    }

    /** Creates a new tag. */
    private void addTag() {
        String text = newTagField.getText() == null ? "" : newTagField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        TermTag tag = termTagService.findOrCreateByText(text);
        if (tag != null) {
            newTagField.clear();
            loadTags();
        }
    }

    /** Changes the text or the comment of the selected tag. */
    private void editTag() {
        TermTag selected = tagsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(selected.getText());
        dialog.setTitle("Editar etiqueta");
        dialog.setHeaderText("Nombre de la etiqueta");
        Optional<String> text = dialog.showAndWait();
        if (text.isEmpty()) {
            return;
        }
        selected.setText(text.get().trim());
        termTagService.save(selected);
        loadTags();
    }

    /** Deletes the selected tag. */
    private void deleteTag() {
        TermTag selected = tagsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        termTagService.delete(selected.getId());
        loadTags();
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
