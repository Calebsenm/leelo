package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Word;
import com.leelo.service.WordService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class wordsController {
    @FXML private TableView<Word> wordsTable;
    @FXML private TableColumn<Word, String> termCol;
    @FXML private TableColumn<Word, String> translationCol;
    @FXML private TableColumn<Word, String> pronunciationCol;
    @FXML private TableColumn<Word, Integer> stateCol;
    @FXML private TextField searchWord;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button practiceButton;
    @FXML private Button backButton;
    @FXML private Button homeButton;
    @FXML private Button textButton;
    @FXML private Button wordsButton;
    @FXML private Button practiceButton2;
    @FXML private Button logoutButton;

    private WordService WordService = new WordService();
    private ObservableList<Word> wordsList = FXCollections.observableArrayList();
    private FilteredList<Word> filteredWords;

    @FXML
    public void initialize() {
        // Set up table columns with proper cell value factories
        termCol.setCellValueFactory(new PropertyValueFactory<>("term"));
        translationCol.setCellValueFactory(new PropertyValueFactory<>("translation"));
        pronunciationCol.setCellValueFactory(new PropertyValueFactory<>("pronunciation"));
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
        
        // Load words and set up filtering
        loadWords();
        setupSearchFilter();
        
        // Set up button actions
        addButton.setOnAction(e -> goToAddWord());
        editButton.setOnAction(e -> editSelectedWord());
        deleteButton.setOnAction(e -> deleteSelectedWord());
        practiceButton.setOnAction(e -> startPractice());
        backButton.setOnAction(e -> goToHome());
    }

    private void loadWords() {
        List<Word> words = WordService.listWords();
        wordsList.setAll(words);
        
        // Set up filtered list for search functionality
        filteredWords = new FilteredList<>(wordsList, p -> true);
        wordsTable.setItems(filteredWords);
    }
    
    private void setupSearchFilter() {
        // Add listener to search field for real-time filtering
        searchWord.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredWords.setPredicate(word -> {
                // If filter text is empty, display all words
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                // Compare term, translation, and pronunciation with filter text
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (word.getTerm() != null && word.getTerm().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches term
                }
                if (word.getTranslation() != null && word.getTranslation().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches translation
                }
                if (word.getPronunciation() != null && word.getPronunciation().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches pronunciation
                }
                
                return false; // Does not match
            });
        });
    }
    
    private void startPractice() {
        try {
            App.setRoot("practice");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToAddWord() {
        try {
            App.setRoot("add_word");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editSelectedWord() {
        Word selected = wordsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("add_word.fxml"));
            Parent root = loader.load();

            addWordController controller = loader.getController();
            controller.setWordToEdit(selected);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add/Edit Word");
            dialog.setScene(new Scene(root, 250, 327));
            dialog.showAndWait();

            loadWords();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedWord() {
        Word selected = wordsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        boolean ok = WordService.deleteWord(selected.getIdTerm());
        if (ok) {
            loadWords();
        }
    }

    private void goToHome() {
        try {
            App.setRoot("home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 