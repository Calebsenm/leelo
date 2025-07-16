package com.leelo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.leelo.App;

public class SideMenuController {
    @FXML
    private Button homeButton;
    @FXML
    private Button textButton;
    @FXML
    private Button wordsButton;
    @FXML
    private Button practiceButton;
    
    private Button activeButton;
    private static final double ICON_SIZE = 20.0;

    @FXML
    public void initialize() {
        setupButtons();
        // Set home button as active by default
        setActiveButton(homeButton);
    }
    
    private void setupButtons() {
        // Setup PNG icons and event handlers
        setupButton(homeButton, "/com/leelo/icons/home.png", "home");
        setupButton(textButton, "/com/leelo/icons/notes.png", "texts");
        setupButton(wordsButton, "/com/leelo/icons/word.png", "words");
        setupButton(practiceButton, "/com/leelo/icons/brain.png", "practice");
    }
    
    private void setupButton(Button button, String iconPath, String destination) {
        try {
            // Load image from resources
            Image image = new Image(getClass().getResourceAsStream(iconPath));
            ImageView imageView = new ImageView(image);
            
            // Set image size
            imageView.setFitWidth(ICON_SIZE);
            imageView.setFitHeight(ICON_SIZE);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            // Set image as button graphic
            button.setGraphic(imageView);
            button.setText(""); // Remove any text
            
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconPath + " - " + e.getMessage());
            // Keep the existing text from FXML as fallback
            // Don't change the text if icon loading fails
        }
        
        // Set navigation action
        button.setOnAction(e -> {
            setActiveButton(button);
            navigate(destination);
        });
    }
    
    private void setActiveButton(Button button) {
        // Reset previous active button
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        
        // Set new active button
        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }
    
    /**
     * Public method to set active button from external controllers
     * This helps maintain the correct selection when navigating between screens
     */
    public void setActiveSection(String section) {
        Button buttonToActivate = null;
        
        switch (section.toLowerCase()) {
            case "home":
                buttonToActivate = homeButton;
                break;
            case "texts":
                buttonToActivate = textButton;
                break;
            case "words":
                buttonToActivate = wordsButton;
                break;
            case "practice":
                buttonToActivate = practiceButton;
                break;
        }
        
        if (buttonToActivate != null) {
            setActiveButton(buttonToActivate);
        }
    }

    private void navigate(String destination) {
        try {
            App.setRoot(destination);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}