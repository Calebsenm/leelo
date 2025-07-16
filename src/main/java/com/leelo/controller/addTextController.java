package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Text;
import com.leelo.service.TextService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class addTextController {

    // Form fields
    @FXML private TextField titleField;
    @FXML private TextArea areaContent;
    @FXML private Button saveButton;
    @FXML private Button backButton;
    
    // UI elements for enhanced functionality
    @FXML private Label formTitleLabel;
    @FXML private Label messageLabel;
    @FXML private Label titleValidationLabel;
    @FXML private Label contentValidationLabel;
    @FXML private Label characterCountLabel;
    @FXML private Label wordCountLabel;
    @FXML private VBox messageContainer;
    @FXML private StackPane loadingContainer;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private HBox buttonContainer;

    private TextService textService = new TextService();
    private Text textToEdit = null;
    private boolean isLoading = false;

    // Method called from text controller for edit 
    public void setTextToEdit(Text text) {
        this.textToEdit = text;
        if (text != null) {
            titleField.setText(text.getTittle());
            areaContent.setText(text.getText());
            formTitleLabel.setText("Edit Text");
            saveButton.setText("Update Text");
            updateCharacterCount();
        }
    }

    @FXML
    public void initialize() {
        setupEventHandlers();
        setupValidation();
        updateCharacterCount();
    }

    private void setupEventHandlers() {
        saveButton.setOnAction(e -> saveOrUpdateText());
        backButton.setOnAction(e -> goToTexts());
        
        // Real-time character and word counting
        areaContent.textProperty().addListener((observable, oldValue, newValue) -> {
            updateCharacterCount();
            clearContentValidation();
        });
        
        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearTitleValidation();
        });
    }

    private void setupValidation() {
        // Add focus listeners for better UX
        titleField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Lost focus
                validateTitle();
            }
        });
        
        areaContent.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Lost focus
                validateContent();
            }
        });
    }

    private void updateCharacterCount() {
        String content = areaContent.getText();
        int charCount = content.length();
        int wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        
        characterCountLabel.setText(charCount + " characters");
        wordCountLabel.setText("• " + wordCount + " words");
    }

    private boolean validateTitle() {
        String title = titleField.getText().trim();
        
        if (title.isEmpty()) {
            showTitleValidation("Title is required", true);
            applyFieldErrorStyle(titleField);
            return false;
        } else if (title.length() < 3) {
            showTitleValidation("Title must be at least 3 characters long", true);
            applyFieldErrorStyle(titleField);
            return false;
        } else if (title.length() > 100) {
            showTitleValidation("Title must be less than 100 characters", true);
            applyFieldErrorStyle(titleField);
            return false;
        } else {
            clearTitleValidation();
            applyFieldSuccessStyle(titleField);
            return true;
        }
    }

    private boolean validateContent() {
        String content = areaContent.getText().trim();
        
        if (content.isEmpty()) {
            showContentValidation("Content is required", true);
            applyFieldErrorStyle(areaContent);
            return false;
        } else if (content.length() < 10) {
            showContentValidation("Content must be at least 10 characters long", true);
            applyFieldErrorStyle(areaContent);
            return false;
        } else {
            clearContentValidation();
            applyFieldSuccessStyle(areaContent);
            return true;
        }
    }

    private void showTitleValidation(String message, boolean isError) {
        titleValidationLabel.setText(message);
        titleValidationLabel.setVisible(true);
        titleValidationLabel.getStyleClass().clear();
        titleValidationLabel.getStyleClass().add(isError ? "validation-message-error" : "validation-message-success");
    }

    private void clearTitleValidation() {
        titleValidationLabel.setVisible(false);
        clearFieldStyle(titleField);
    }

    private void showContentValidation(String message, boolean isError) {
        contentValidationLabel.setText(message);
        contentValidationLabel.setVisible(true);
        contentValidationLabel.getStyleClass().clear();
        contentValidationLabel.getStyleClass().add(isError ? "validation-message-error" : "validation-message-success");
    }

    private void clearContentValidation() {
        contentValidationLabel.setVisible(false);
        clearFieldStyle(areaContent);
    }

    private void applyFieldErrorStyle(Control field) {
        field.getStyleClass().removeAll("text-field-success", "text-area-success");
        if (field instanceof TextField) {
            field.getStyleClass().add("text-field-error");
        } else if (field instanceof TextArea) {
            field.getStyleClass().add("text-area-error");
        }
    }

    private void applyFieldSuccessStyle(Control field) {
        field.getStyleClass().removeAll("text-field-error", "text-area-error");
        if (field instanceof TextField) {
            field.getStyleClass().add("text-field-success");
        } else if (field instanceof TextArea) {
            field.getStyleClass().add("text-area-success");
        }
    }

    private void clearFieldStyle(Control field) {
        field.getStyleClass().removeAll("text-field-error", "text-area-error", "text-field-success", "text-area-success");
    }

    private void saveOrUpdateText() {
        if (isLoading) return;
        
        // Validate all fields
        boolean titleValid = validateTitle();
        boolean contentValid = validateContent();
        
        if (!titleValid || !contentValid) {
            showMessage("Please fix the validation errors above", MessageType.ERROR);
            return;
        }

        // Show loading state
        setLoadingState(true);
        
        // Create background task for saving
        Task<Boolean> saveTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Simulate some processing time for better UX
                Thread.sleep(500);
                
                String title = titleField.getText().trim();
                String content = areaContent.getText().trim();
                
                boolean success;
                if (textToEdit != null) {
                    textToEdit.setTittle(title);
                    textToEdit.setText(content);
                    success = textService.updateText(textToEdit);
                } else {
                    Text text = new Text();
                    text.setTittle(title);
                    text.setText(content);
                    success = textService.addText(text);
                }
                
                return success;
            }
        };
        
        saveTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                setLoadingState(false);
                boolean success = saveTask.getValue();
                
                if (success) {
                    String action = textToEdit != null ? "updated" : "saved";
                    showMessage("Text " + action + " successfully!", MessageType.SUCCESS);
                    
                    // Apply success styles to fields
                    applyFieldSuccessStyle(titleField);
                    applyFieldSuccessStyle(areaContent);
                    
                    // Navigate back after a short delay
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(1000);
                            goToTexts();
                        } catch (InterruptedException ex) {
                            goToTexts();
                        }
                    });
                } else {
                    showMessage("Could not save the text. Please try again.", MessageType.ERROR);
                }
            });
        });
        
        saveTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setLoadingState(false);
                showMessage("An error occurred while saving. Please try again.", MessageType.ERROR);
            });
        });
        
        // Run the task in background thread
        Thread saveThread = new Thread(saveTask);
        saveThread.setDaemon(true);
        saveThread.start();
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        loadingContainer.setVisible(loading);
        loadingContainer.setManaged(loading);
        buttonContainer.setVisible(!loading);
        buttonContainer.setManaged(!loading);
        
        // Disable form fields during loading
        titleField.setDisable(loading);
        areaContent.setDisable(loading);
    }

    private void goToTexts() {
        try {
            App.setRoot("texts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private enum MessageType {
        SUCCESS, ERROR, WARNING, INFO
    }

    private void showMessage(String message, MessageType type) {
        messageLabel.setText(message);
        messageContainer.setVisible(true);
        
        // Clear existing style classes
        messageLabel.getStyleClass().removeAll(
            "validation-message-success", 
            "validation-message-error", 
            "validation-message-warning", 
            "validation-message-info"
        );
        
        // Apply appropriate style class
        switch (type) {
            case SUCCESS:
                messageLabel.getStyleClass().add("validation-message-success");
                break;
            case ERROR:
                messageLabel.getStyleClass().add("validation-message-error");
                break;
            case WARNING:
                messageLabel.getStyleClass().add("validation-message-warning");
                break;
            case INFO:
                messageLabel.getStyleClass().add("validation-message-info");
                break;
        }
        
        // Auto-hide success messages after 3 seconds
        if (type == MessageType.SUCCESS) {
            Platform.runLater(() -> {
                try {
                    Thread.sleep(3000);
                    if (messageContainer.isVisible()) {
                        messageContainer.setVisible(false);
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
            });
        }
    }
} 