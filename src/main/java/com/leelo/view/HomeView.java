package com.leelo.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class HomeView extends VBox {
    
    private final VBox bookCard;
    private final Label bookIcon;
    private final Label bookTitleLabel;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label wordCountLabel;
    private final Button addButton;
    
    private VBox emptyStateCard;
    
    public HomeView() {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #f5f5f5;");
        
        // Título de bienvenida
        Label welcomeTitle = new Label("Bienvenido a Leelo");
        welcomeTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Label subtitle = new Label("Aplicación de aprendizaje de lectura y vocabulario");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        VBox headerBox = new VBox(10);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getChildren().addAll(welcomeTitle, subtitle);
        
        // Card del libro
        bookCard = createBookCard();
        
        // Extraer componentes para acceso
        bookIcon = (Label) ((HBox) bookCard.getChildren().get(0)).getChildren().get(0);
        VBox infoBox = (VBox) ((HBox) bookCard.getChildren().get(0)).getChildren().get(1);
        bookTitleLabel = (Label) infoBox.getChildren().get(0);
        wordCountLabel = (Label) infoBox.getChildren().get(1);
        HBox progressBox = (HBox) infoBox.getChildren().get(2);
        progressBar = (ProgressBar) progressBox.getChildren().get(0);
        progressLabel = (Label) progressBox.getChildren().get(1);
        
        // Card de estado vacío
        emptyStateCard = createEmptyStateCard();
        
        // Sección de funciones
        VBox featuresBox = createFeaturesSection();
        
        // Footer
        Label footer = new Label("Leelo v1.0");
        footer.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        
        // Botón flotante de agregar
        addButton = createAddButton();
        
        getChildren().addAll(headerBox, bookCard, featuresBox, footer);
    }
    
    private VBox createBookCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setMaxWidth(600);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );
        
        HBox content = new HBox(20);
        content.setAlignment(Pos.CENTER_LEFT);
        
        // Icono del libro
        Label icon = createBookIcon();
        
        // Info del libro
        VBox info = new VBox(8);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label title = new Label("Sin libro");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Label wordCount = new Label("0 palabras");
        wordCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
        
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(300);
        progress.setStyle(
            "-fx-accent: #4CAF50;" +
            "-fx-control-inner-background: #e0e0e0;"
        );
        HBox.setHgrow(progress, Priority.ALWAYS);
        
        Label progressText = new Label("0%");
        progressText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #666;");
        
        progressBox.getChildren().addAll(progress, progressText);
        info.getChildren().addAll(title, wordCount, progressBox);
        
        content.getChildren().addAll(icon, info);
        card.getChildren().add(content);
        
        // Hacer clickeable
        card.setOnMouseClicked(e -> onBookCardClick());
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #fafafa;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 3);" +
            "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        ));
        
        return card;
    }
    
    private Label createBookIcon() {
        Label icon = new Label("📖");
        icon.setStyle(
            "-fx-font-size: 80px;" +
            "-fx-background-color: #f0f0f0;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 20;" +
            "-fx-min-width: 120px;" +
            "-fx-min-height: 120px;" +
            "-fx-alignment: center;"
        );
        return icon;
    }
    
    private VBox createEmptyStateCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(40));
        card.setMaxWidth(600);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );
        
        Label emptyIcon = new Label("📚");
        emptyIcon.setStyle("-fx-font-size: 80px;");
        
        Label emptyTitle = new Label("No hay libros aún");
        emptyTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Label emptyMessage = new Label("Agrega tu primer libro para comenzar a leer");
        emptyMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        emptyMessage.setWrapText(true);
        
        card.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        card.setVisible(false);
        card.setManaged(false);
        
        return card;
    }
    
    private VBox createFeaturesSection() {
        VBox features = new VBox(15);
        features.setAlignment(Pos.CENTER);
        features.setMaxWidth(600);
        features.setPadding(new Insets(20));
        features.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );
        
        Label featuresTitle = new Label("Funciones principales");
        featuresTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        VBox featuresList = new VBox(8);
        featuresList.setAlignment(Pos.CENTER_LEFT);
        
        String[] featureTexts = {
            "• Gestionar Textos — Organiza tu biblioteca de textos de lectura",
            "• Aprender Palabras — Construye tu vocabulario con nuevas palabras",
            "• Practicar — Refuerza tu aprendizaje con ejercicios"
        };
        
        for (String text : featureTexts) {
            Label feature = new Label(text);
            feature.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
            feature.setWrapText(true);
            featuresList.getChildren().add(feature);
        }
        
        features.getChildren().addAll(featuresTitle, featuresList);
        return features;
    }
    
    private Button createAddButton() {
        Button btn = new Button("+");
        btn.setStyle(
            "-fx-background-color: #2196F3;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Arial';" +
            "-fx-background-radius: 30px;" +
            "-fx-min-width: 60px;" +
            "-fx-min-height: 60px;" +
            "-fx-max-width: 60px;" +
            "-fx-max-height: 60px;" +
            "-fx-pref-width: 60px;" +
            "-fx-pref-height: 60px;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #1976D2;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Arial';" +
            "-fx-background-radius: 30px;" +
            "-fx-min-width: 60px;" +
            "-fx-min-height: 60px;" +
            "-fx-max-width: 60px;" +
            "-fx-max-height: 60px;" +
            "-fx-pref-width: 60px;" +
            "-fx-pref-height: 60px;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);" +
            "-fx-cursor: hand;"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #2196F3;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Arial';" +
            "-fx-background-radius: 30px;" +
            "-fx-min-width: 60px;" +
            "-fx-min-height: 60px;" +
            "-fx-max-width: 60px;" +
            "-fx-max-height: 60px;" +
            "-fx-pref-width: 60px;" +
            "-fx-pref-height: 60px;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);"
        ));
        
        btn.setOnAction(e -> onAddButtonClick());
        
        return btn;
    }
    
    protected void onBookCardClick() {
        // Override en el controlador
    }
    
    protected void onAddButtonClick() {
        // Override en el controlador
    }
    
    public void updateBookInfo(String title, int progress, int wordCount) {
        bookTitleLabel.setText(title);
        progressBar.setProgress(progress / 100.0);
        progressLabel.setText(progress + "%");
        wordCountLabel.setText(formatWordCount(wordCount));
    }
    
    private String formatWordCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fk palabras", count / 1000.0);
        }
        return count + " palabras";
    }
    
    public void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            bookCard.setVisible(false);
            bookCard.setManaged(false);
            emptyStateCard.setVisible(true);
            emptyStateCard.setManaged(true);
            
            // Agregar el card vacío si no está en la vista
            if (!getChildren().contains(emptyStateCard)) {
                getChildren().add(1, emptyStateCard);
            }
        } else {
            bookCard.setVisible(true);
            bookCard.setManaged(true);
            emptyStateCard.setVisible(false);
            emptyStateCard.setManaged(false);
        }
    }
    
    public void setBookCardClickHandler(Runnable handler) {
        bookCard.setOnMouseClicked(e -> handler.run());
    }
    
    public void setAddButtonClickHandler(Runnable handler) {
        addButton.setOnAction(e -> handler.run());
    }
    
    public Button getAddButton() {
        return addButton;
    }
}
