package com.leelo;

import com.leelo.dao.Database;
import com.leelo.util.ResponsiveManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class App extends Application {

    private static Scene scene;
    private static ResponsiveManager responsiveManager;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("home"), 720, 480);
        stage.setScene(scene);
        stage.setTitle("Leelo - Main Menu");
        
        // Initialize responsive design manager
        responsiveManager = ResponsiveManager.createAndInitialize(stage);
        
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        
        // Ensure new root is managed by responsive system
        if (responsiveManager != null && scene.getRoot() != null) {
            responsiveManager.addManagedNode(scene.getRoot());
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        try {
            // Solo crea la base de datos y las tablas si no existen
            Database.initialize();

            // Lanza la aplicación JavaFX
            launch();

        } catch (Exception e) {
            System.err.println("Critical error during application startup: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                showErrorDialog("Startup Error",
                    "A critical error occurred during application startup.",
                    e.getMessage());
            });

            System.exit(1);
        }
    }

    public static Scene getScene() {
        return scene;
    }
    
    /**
     * Get the responsive manager instance
     * @return ResponsiveManager instance or null if not initialized
     */
    public static ResponsiveManager getResponsiveManager() {
        return responsiveManager;
    }

    /**
     * Shows an error dialog to the user
     * @param title Dialog title
     * @param header Header text
     * @param content Content text
     */
    private static void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows an information dialog to the user
     * @param title Dialog title
     * @param header Header text
     * @param content Content text
     */
    private static void showInfoDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

}