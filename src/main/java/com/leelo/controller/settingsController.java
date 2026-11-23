package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Language;
import com.leelo.service.HotkeyService;
import com.leelo.service.LanguageService;
import com.leelo.service.SettingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import com.leelo.service.OllamaService;
import javafx.concurrent.Task;

import java.util.List;

/**
 * The settings of the user and the keyboard shortcuts of the reading screen.
 */
public class settingsController {

    @FXML
    private ComboBox<Language> languageCombo;
    @FXML
    private CheckBox showHighlightsCheck;
    @FXML
    private CheckBox showStreakCheck;
    @FXML
    private CheckBox promoteParentCheck;
    @FXML
    private CheckBox showComponentsCheck;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private Spinner<Integer> sampleSizeSpinner;
    @FXML
    private TableView<HotkeyService.Action> hotkeysTable;
    @FXML
    private TableColumn<HotkeyService.Action, String> categoryCol;
    @FXML
    private TableColumn<HotkeyService.Action, String> actionCol;
    @FXML
    private TableColumn<HotkeyService.Action, String> shortcutCol;
    @FXML
    private TextField ollamaUrlField;
    @FXML
    private TextField ollamaModelField;
    @FXML
    private CheckBox ollamaAutoTranslateCheck;
    @FXML
    private Button testOllamaButton;
    @FXML
    private Label ollamaStatusLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private VBox messageContainer;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;

    private final SettingService settingService = new SettingService();
    private final LanguageService languageService = new LanguageService();
    private final HotkeyService hotkeyService = new HotkeyService(settingService);
    private final OllamaService ollamaService = new OllamaService();

    private final ObservableList<HotkeyService.Action> actions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        languageCombo.getItems().setAll(languageService.getSupported());

        fontSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 48, 25, 1));
        sampleSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 5, 1));

        categoryCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCategory().getLabel()));
        actionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        shortcutCol.setCellValueFactory(cellData -> {
            HotkeyService.Action action = cellData.getValue();
            return new SimpleStringProperty(HotkeyService.toDisplayName(hotkeyService.getCombination(action)));
        });

        actions.setAll(HotkeyService.Action.values());
        hotkeysTable.setItems(actions);
        hotkeysTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editShortcut();
            }
        });

        saveButton.setOnAction(event -> save());
        backButton.setOnAction(event -> goToHome());
        testOllamaButton.setOnAction(event -> testOllamaConnection());

        loadSettings();
    }

    /** Fills the form with the current settings. */
    private void loadSettings() {
        languageCombo.setValue(languageService.getById(settingService.getCurrentLanguageId()));
        showHighlightsCheck.setSelected(settingService.getBoolean(SettingService.SHOW_HIGHLIGHTS));
        showStreakCheck.setSelected(settingService.getBoolean(SettingService.SHOW_STREAK_ON_HOME));
        promoteParentCheck.setSelected(settingService.getBoolean(SettingService.PROMOTE_PARENT_TRANSLATION));
        showComponentsCheck.setSelected(settingService.getBoolean(SettingService.SHOW_TERM_COMPONENTS));
        fontSizeSpinner.getValueFactory().setValue(settingService.getInt(SettingService.FONT_SIZE));
        sampleSizeSpinner.getValueFactory().setValue(settingService.getInt(SettingService.STATS_SAMPLE_SIZE));
        ollamaUrlField.setText(settingService.getRaw(SettingService.OLLAMA_URL));
        String model = settingService.getRaw(SettingService.OLLAMA_MODEL);
        if (model == null || model.isBlank() || "llama3.2".equalsIgnoreCase(model.trim())) {
            model = "qwen2.5:3b";
            settingService.setRaw(SettingService.OLLAMA_MODEL, model);
        }
        ollamaModelField.setText(model);
        ollamaAutoTranslateCheck.setSelected(settingService.getBoolean(SettingService.OLLAMA_AUTO_TRANSLATE));
    }

    /** Saves the settings. */
    private void save() {
        Language language = languageCombo.getValue();
        settingService.setCurrentLanguageId(language == null ? 0 : language.getId());
        settingService.setBoolean(SettingService.SHOW_HIGHLIGHTS, showHighlightsCheck.isSelected());
        settingService.setBoolean(SettingService.SHOW_STREAK_ON_HOME, showStreakCheck.isSelected());
        settingService.setBoolean(SettingService.PROMOTE_PARENT_TRANSLATION, promoteParentCheck.isSelected());
        settingService.setBoolean(SettingService.SHOW_TERM_COMPONENTS, showComponentsCheck.isSelected());
        settingService.setInt(SettingService.FONT_SIZE, fontSizeSpinner.getValue());
        settingService.setInt(SettingService.STATS_SAMPLE_SIZE, sampleSizeSpinner.getValue());
        String url = ollamaUrlField.getText() == null ? "" : ollamaUrlField.getText().trim();
        String model = ollamaModelField.getText() == null ? "" : ollamaModelField.getText().trim();
        settingService.setRaw(SettingService.OLLAMA_URL, url.isEmpty() ? "http://localhost:11434" : url);
        settingService.setRaw(SettingService.OLLAMA_MODEL, model.isEmpty() ? "llama3.2" : model);
        settingService.setBoolean(SettingService.OLLAMA_AUTO_TRANSLATE, ollamaAutoTranslateCheck.isSelected());

        hotkeysTable.refresh();
        showMessage("Ajustes guardados.");
    }

    /** Tests connection to the Ollama server. */
    private void testOllamaConnection() {
        String url = ollamaUrlField.getText() == null || ollamaUrlField.getText().isBlank()
                ? "http://localhost:11434" : ollamaUrlField.getText().trim();
        ollamaStatusLabel.setText("Probando conexión...");
        ollamaStatusLabel.setStyle("-fx-text-fill: #6366f1;");
        testOllamaButton.setDisable(true);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return ollamaService.getInstalledModels(url);
            }
        };

        task.setOnSucceeded(e -> {
            testOllamaButton.setDisable(false);
            List<String> models = task.getValue();
            if (models.isEmpty()) {
                ollamaStatusLabel.setText("✓ Conectado a Ollama, pero no hay modelos instalados ('ollama pull <modelo>').");
                ollamaStatusLabel.setStyle("-fx-text-fill: #eab308;");
            } else {
                ollamaStatusLabel.setText("✓ Conexión exitosa. Modelos: " + String.join(", ", models));
                ollamaStatusLabel.setStyle("-fx-text-fill: #16a34a;");
            }
        });

        task.setOnFailed(e -> {
            testOllamaButton.setDisable(false);
            ollamaStatusLabel.setText("✗ No se pudo conectar a Ollama en " + url + ". Asegúrate de ejecutar 'ollama serve'.");
            ollamaStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        });

        Thread thread = new Thread(task, "leelo-test-ollama");
        thread.setDaemon(true);
        thread.start();
    }

    /** Lets the user pick a new shortcut for the selected action. */
    private void editShortcut() {
        HotkeyService.Action action = hotkeysTable.getSelectionModel().getSelectedItem();
        if (action == null) {
            return;
        }
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Atajo de teclado");
        dialog.setHeaderText("Escribe la combinacion para " + action.getDescription());
        dialog.getEditor().setText(HotkeyService.toDisplayName(hotkeyService.getCombination(action)));
        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        KeyCombination combination = HotkeyService.fromName(result.get().trim());
        if (combination == null) {
            showMessage("La combinacion no es valida.");
            return;
        }
        List<HotkeyService.Action> conflicts = hotkeyService.findConflicts(action, combination);
        if (!conflicts.isEmpty()) {
            showMessage("El atajo ya se usa en: " + conflicts.get(0).getDescription());
            return;
        }
        hotkeyService.setCombination(action, combination);
        hotkeysTable.refresh();
    }

    /** Shows a message below the form. */
    private void showMessage(String message) {
        messageLabel.setText(message);
        messageContainer.setVisible(true);
        messageLabel.getStyleClass().removeAll("text-success", "text-error");
        messageLabel.getStyleClass().add("text-success");
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
