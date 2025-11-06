package com.leelo.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class addUrlController {

    @FXML
    private TextField titleField;
    @FXML
    private TextField urlField;
    @FXML
    private Button saveButton;

    private addTextController parent;

    public void setParentController(addTextController parent) {
        this.parent = parent;
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> saveUrl());
    }

    private void saveUrl() {
        String title = titleField.getText().trim();
        String url = urlField.getText().trim();

        if (title.isBlank() || url.isBlank()) {
            System.out.println("Debe llenar ambos campos");
            return;
        }

        // Intentar leer el contenido de la página
        try {
            System.out.println("Leyendo contenido de la página: " + url);
            Document doc = Jsoup.connect(url).get();

            // Extraer texto visible de la página
            String text = doc.body().text();

            // Enviar título y texto al controlador principal
            if (parent != null) {
                parent.addTextFromUrl(title, text);
            }

            System.out.println("Texto extraído correctamente de: " + title);

        } catch (Exception e) {
            System.err.println("Error al leer la página: " + e.getMessage());
        }

        // Cerrar ventana
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
