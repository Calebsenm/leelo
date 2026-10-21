package com.leelo;

import com.leelo.controller.HomeController;
import com.leelo.controller.ReadingController;
import com.leelo.controller.SideMenuController;
import com.leelo.dao.Database;
import com.leelo.dao.LegacyDataBridge;
import com.leelo.model.Book;
import com.leelo.service.SettingService;
import com.leelo.util.ResponsiveManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The entry point of the application.
 *
 * <p>It owns the scene and knows how to move between the screens.
 */
public class App extends Application {

    private static Scene scene;
    private static ResponsiveManager responsiveManager;

    @Override
    public void start(Stage stage) throws IOException {
        Database.initialize();
        LegacyDataBridge.importIfNeeded();
        new SettingService().initializeDefaults();

        scene = new Scene(createHome(), 1180, 760);
        scene.getStylesheets().add(App.class.getResource("css/main.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Leelo");
        stage.setMinWidth(980);
        stage.setMinHeight(640);

        responsiveManager = ResponsiveManager.createAndInitialize(stage);
        stage.show();
    }

    /** Builds the home screen, with the navigation bar on the left. */
    private static Parent createHome() throws IOException {
        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        root.getStyleClass().add("app-shell");
        root.setLeft(loadSideMenu());
        root.setCenter(new HomeController().getView());
        return root;
    }

    /** Loads the navigation bar. */
    private static javafx.scene.layout.VBox loadSideMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("side_menu.fxml"));
        return loader.load();
    }

    /** Shows the home screen. */
    public static void showHome() throws IOException {
        SideMenuController.setCurrentSection("home");
        scene.setRoot(createHome());
        if (responsiveManager != null && scene.getRoot() != null) {
            responsiveManager.addManagedNode(scene.getRoot());
        }
    }

    /** Replaces the current screen with the one of the given file. */
    public static void setRoot(String fxml) throws IOException {
        SideMenuController.setCurrentSection(fxml);
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        scene.setRoot(loader.load());
        if (responsiveManager != null && scene.getRoot() != null) {
            responsiveManager.addManagedNode(scene.getRoot());
        }
    }

    /** Opens a book in the reading screen. */
    public static void openReading(Book book) throws IOException {
        SideMenuController.setCurrentSection("texts");
        FXMLLoader loader = new FXMLLoader(App.class.getResource("reading.fxml"));
        Parent root = loader.load();
        ReadingController controller = loader.getController();
        controller.setBook(book);
        scene.setRoot(root);
        Stage stage = (Stage) scene.getWindow();
        if (stage != null) {
            stage.setMaximized(true);
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

    public static void main(String[] args) {
        try {
            launch();
        } catch (Exception e) {
            System.err.println("Critical error during application startup: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Startup Error");
                alert.setHeaderText("A critical error occurred during application startup.");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });

            System.exit(1);
        }
    }
}
