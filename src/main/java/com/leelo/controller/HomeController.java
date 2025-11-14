package com.leelo.controller;

import com.leelo.view.HomeView;
import com.leelo.viewmodel.HomeViewModel;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class HomeController {
    
    private HomeViewModel viewModel;
    private HomeView view;
    
    @FXML
    public void initialize() {
        this.viewModel = new HomeViewModel();
        this.view = new HomeView();
        
        bindViewModel();
    }
    
    private void bindViewModel() {
        // Actualizar vista cuando cambie el viewmodel
        viewModel.bookTitleProperty().addListener((obs, oldVal, newVal) -> {
            view.updateBookInfo(newVal, viewModel.getReadingProgress(), viewModel.getWordCount());
        });
        
        viewModel.readingProgressProperty().addListener((obs, oldVal, newVal) -> {
            view.updateBookInfo(viewModel.getBookTitle(), newVal.intValue(), viewModel.getWordCount());
        });
        
        viewModel.wordCountProperty().addListener((obs, oldVal, newVal) -> {
            view.updateBookInfo(viewModel.getBookTitle(), viewModel.getReadingProgress(), newVal.intValue());
        });
        
        viewModel.hasBookProperty().addListener((obs, oldVal, newVal) -> {
            view.showEmptyState(!newVal);
        });
        
        // Configurar handlers
        view.setBookCardClickHandler(this::handleOpenBook);
        view.setAddButtonClickHandler(this::handleAddBook);
        
        // Inicializar vista
        view.updateBookInfo(viewModel.getBookTitle(), viewModel.getReadingProgress(), viewModel.getWordCount());
        view.showEmptyState(!viewModel.hasBook());
    }
    
    private void handleOpenBook() {
        if (viewModel.hasBook() && viewModel.getLastOpenedBook() != null) {
            try {
                // Cargar la vista de lectura con el libro
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/leelo/reading.fxml")
                );
                javafx.scene.layout.BorderPane readingRoot = loader.load();
                
                // Obtener el controlador y pasarle el texto
                ReadingController readingController = loader.getController();
                readingController.setText(viewModel.getLastOpenedBook());
                
                // Cambiar la escena
                com.leelo.App.getScene().setRoot(readingRoot);
            } catch (Exception e) {
                System.err.println("Error al abrir libro: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleAddBook() {
        try {
            com.leelo.App.setRoot("add_text");
        } catch (Exception e) {
            System.err.println("Error al navegar a agregar libro: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public StackPane getView() {
        StackPane root = new StackPane();
        root.getChildren().add(view);
        root.getChildren().add(view.getAddButton());
        StackPane.setAlignment(view.getAddButton(), javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(view.getAddButton(), new javafx.geometry.Insets(0, 30, 30, 0));
        return root;
    }
} 