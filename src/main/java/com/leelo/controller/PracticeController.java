package com.leelo.controller;

import com.leelo.model.Word;
import com.leelo.service.WordService;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

public class PracticeController {
    private static final int CORRECT_AUTO_ADVANCE_DELAY_MS = 1400;
    private static final int DEFAULT_SESSION_WORD_COUNT = 30;
    private static final int MIN_SESSION_WORD_COUNT = 1;
    private static final int MAX_SESSION_WORD_COUNT = 200;

    private enum PracticeMode {
        DESAFIO("Modo Desafio (Cloze)"),
        RELAJADO("Modo Seleccion Rapida");

        private final String label;

        PracticeMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @FXML private Label wordLabel;
    @FXML private Label translationLabel;
    @FXML private Label pronunciationLabel;
    @FXML private Label modeDescriptionLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label progressLabel;
    @FXML private Label sessionStatsLabel;
    @FXML private Button nextButton;
    @FXML private Button startSessionButton;
    @FXML private Button endSessionButton;
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<PracticeMode> modeCombo;
    @FXML private Spinner<Integer> wordCountSpinner;
    @FXML private VBox challengeBox;
    @FXML private TextField challengeInput;
    @FXML private Button submitAnswerButton;
    @FXML private TilePane choiceBox;
    @FXML private Button choiceButton1;
    @FXML private Button choiceButton2;
    @FXML private Button choiceButton3;
    @FXML private Button choiceButton4;
    @FXML private Button continueButton;
    @FXML private SideMenuController menuController;

    private final WordService wordService = new WordService();

    private List<Word> sessionWords = new ArrayList<>();
    private Word currentWord;
    private int currentIndex;
    private int wordsReviewed;
    private int correctAnswers;
    private boolean transitioning;
    private boolean waitingManualAdvance;
    private PauseTransition autoAdvanceTransition;

    @FXML
    public void initialize() {
        if (menuController != null) {
            menuController.setActiveSection("practice");
        }

        modeCombo.setItems(FXCollections.observableArrayList(PracticeMode.values()));
        modeCombo.setValue(PracticeMode.DESAFIO);
        modeCombo.valueProperty().addListener((obs, oldV, newV) -> refreshModeView());
        configureWordCountSpinner();
        challengeInput.setOnAction(e -> submitChallengeAnswer());

        resetSessionState();
        updateProgressDisplay();
        updateSessionStatsDisplay();
        setInitialUiState();
    }

    @FXML
    public void startStudySession() {
        sessionWords = buildPracticeWordList();
        currentIndex = 0;
        wordsReviewed = 0;
        correctAnswers = 0;
        transitioning = false;
        waitingManualAdvance = false;

        if (sessionWords.isEmpty()) {
            wordLabel.setText("No hay palabras para practicar");
            translationLabel.setText("Agrega palabras con significado para empezar.");
            translationLabel.setVisible(true);
            translationLabel.setManaged(true);
            feedbackLabel.setVisible(false);
            feedbackLabel.setManaged(false);
            return;
        }

        startSessionButton.setVisible(false);
        startSessionButton.setManaged(false);
        wordCountSpinner.setDisable(true);
        nextButton.setVisible(true);
        nextButton.setManaged(true);
        endSessionButton.setVisible(true);
        endSessionButton.setManaged(true);

        showCurrentWord();
    }

    @FXML
    public void submitChallengeAnswer() {
        if (currentWord == null || transitioning) {
            return;
        }

        String typed = challengeInput.getText() != null ? challengeInput.getText().trim() : "";
        boolean correct = normalize(typed).equals(normalize(currentWord.getTerm()));
        handleAnswerResult(correct);
    }

    @FXML
    public void selectChoice1() { selectChoice(choiceButton1); }
    @FXML
    public void selectChoice2() { selectChoice(choiceButton2); }
    @FXML
    public void selectChoice3() { selectChoice(choiceButton3); }
    @FXML
    public void selectChoice4() { selectChoice(choiceButton4); }

    private void selectChoice(Button button) {
        if (currentWord == null || button.getText() == null || transitioning) {
            return;
        }
        boolean correct = normalize(button.getText()).equals(normalize(currentWord.getTerm()));
        highlightChoiceResult(button, correct);
        handleAnswerResult(correct);
    }

    @FXML
    public void skipWord() {
        if (currentWord == null || transitioning) {
            return;
        }
        currentIndex++;
        showCurrentWord();
    }

    @FXML
    public void continueAfterFeedback() {
        if (!transitioning || !waitingManualAdvance) {
            return;
        }
        proceedToNextWord();
    }

    @FXML
    public void endStudySession() {
        if (sessionWords.isEmpty()) {
            return;
        }

        wordLabel.setText("Sesion completada");
        double accuracy = wordsReviewed == 0 ? 0.0 : (double) correctAnswers * 100.0 / wordsReviewed;
        translationLabel.setText(String.format("Revisadas: %d | Correctas: %d | Precision: %.1f%%", wordsReviewed, correctAnswers, accuracy));
        translationLabel.setVisible(true);
        translationLabel.setManaged(true);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        resetSessionState();
        setInitialUiState();
        updateProgressDisplay();
        updateSessionStatsDisplay();
    }

    private void handleAnswerResult(boolean correct) {
        transitioning = true;
        setInteractionDisabled(true);

        wordsReviewed++;
        if (correct) {
            correctAnswers++;
            translationLabel.setText("Correcto: " + currentWord.getTranslation());
            showFeedback("Bien hecho", true);
        } else {
            translationLabel.setText("Incorrecto. Respuesta: " + currentWord.getTerm() + " = " + currentWord.getTranslation());
            showFeedback("Respuesta incorrecta", false);
        }
        translationLabel.setVisible(true);
        translationLabel.setManaged(true);

        if (currentWord.getPronunciation() != null && !currentWord.getPronunciation().isBlank()) {
            pronunciationLabel.setText("[" + currentWord.getPronunciation() + "]");
            pronunciationLabel.setVisible(true);
            pronunciationLabel.setManaged(true);
        }

        updateProgressDisplay();
        updateSessionStatsDisplay();

        if (!correct) {
            waitingManualAdvance = true;
            continueButton.setVisible(true);
            continueButton.setManaged(true);
            continueButton.setDisable(false);
            modeDescriptionLabel.setText("Incorrecto. Revisa la respuesta y presiona Continuar.");
            return;
        }

        waitingManualAdvance = false;
        modeDescriptionLabel.setText("Correcto. Avanzando a la siguiente palabra...");
        autoAdvanceTransition = new PauseTransition(Duration.millis(CORRECT_AUTO_ADVANCE_DELAY_MS));
        autoAdvanceTransition.setOnFinished(e -> proceedToNextWord());
        autoAdvanceTransition.play();
    }

    private void proceedToNextWord() {
        if (autoAdvanceTransition != null) {
            autoAdvanceTransition.stop();
            autoAdvanceTransition = null;
        }
        continueButton.setVisible(false);
        continueButton.setManaged(false);
        currentIndex++;
        transitioning = false;
        waitingManualAdvance = false;
        setInteractionDisabled(false);
        showCurrentWord();
    }

    private void showCurrentWord() {
        if (currentIndex >= sessionWords.size()) {
            endStudySession();
            return;
        }

        currentWord = sessionWords.get(currentIndex);

        pronunciationLabel.setVisible(false);
        pronunciationLabel.setManaged(false);
        choiceBox.setVisible(false);
        choiceBox.setManaged(false);
        challengeBox.setVisible(false);
        challengeBox.setManaged(false);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        continueButton.setVisible(false);
        continueButton.setManaged(false);
        clearChoiceStyles();
        challengeInput.clear();

        updateProgressDisplay();
        updateSessionStatsDisplay();
        refreshModeView();
        Platform.runLater(() -> {
            if (modeCombo.getValue() == PracticeMode.DESAFIO) {
                challengeInput.requestFocus();
            }
        });
    }

    private void refreshModeView() {
        PracticeMode mode = modeCombo.getValue() != null ? modeCombo.getValue() : PracticeMode.DESAFIO;
        if (currentWord == null) {
            return;
        }

        challengeBox.setVisible(false);
        challengeBox.setManaged(false);
        choiceBox.setVisible(false);
        choiceBox.setManaged(false);
        clearChoiceStyles();
        challengeInput.clear();

        switch (mode) {
            case DESAFIO:
                wordLabel.setText("Completa la palabra para:");
                modeDescriptionLabel.setText("Escribe la palabra correcta. Si aciertas, avanza sola.");
                translationLabel.setText(currentWord.getTranslation());
                translationLabel.setVisible(true);
                translationLabel.setManaged(true);
                challengeBox.setVisible(true);
                challengeBox.setManaged(true);
                break;

            case RELAJADO:
                wordLabel.setText("Selecciona la palabra correcta para:");
                modeDescriptionLabel.setText("Elige la opcion correcta. Si aciertas, avanza sola.");
                translationLabel.setText(currentWord.getTranslation());
                translationLabel.setVisible(true);
                translationLabel.setManaged(true);
                buildMultipleChoiceOptions();
                choiceBox.setVisible(true);
                choiceBox.setManaged(true);
                break;
        }
    }

    private void buildMultipleChoiceOptions() {
        List<String> options = new ArrayList<>();
        options.add(currentWord.getTerm());

        List<String> distractors = new ArrayList<>();
        for (Word word : sessionWords) {
            if (word == currentWord) {
                continue;
            }
            if (word.getTerm() != null && !word.getTerm().isBlank()) {
                distractors.add(word.getTerm());
            }
        }

        Collections.shuffle(distractors);
        for (String d : distractors) {
            if (options.size() >= 4) {
                break;
            }
            if (!containsIgnoreCase(options, d)) {
                options.add(d);
            }
        }

        while (options.size() < 4) {
            options.add(currentWord.getTerm());
        }

        Collections.shuffle(options);
        choiceButton1.setText(options.get(0));
        choiceButton2.setText(options.get(1));
        choiceButton3.setText(options.get(2));
        choiceButton4.setText(options.get(3));
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        for (String item : list) {
            if (item != null && item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private List<Word> buildPracticeWordList() {
        Set<String> seen = new LinkedHashSet<>();
        List<Word> words = new ArrayList<>();

        for (Word word : wordService.listWords()) {
            if (!isValidWord(word)) {
                continue;
            }
            String key = normalize(word.getTerm());
            if (!seen.contains(key)) {
                seen.add(key);
                words.add(word);
            }
        }

        Collections.shuffle(words);
        int maxSessionWords = Math.min(words.size(), getRequestedWordCount());
        return new ArrayList<>(words.subList(0, maxSessionWords));
    }

    private boolean isValidWord(Word word) {
        return word != null
                && word.getTerm() != null && !word.getTerm().trim().isEmpty()
                && word.getTranslation() != null && !word.getTranslation().trim().isEmpty();
    }

    private void resetSessionState() {
        sessionWords.clear();
        currentWord = null;
        currentIndex = 0;
        wordsReviewed = 0;
        correctAnswers = 0;
        transitioning = false;
        waitingManualAdvance = false;
        if (autoAdvanceTransition != null) {
            autoAdvanceTransition.stop();
            autoAdvanceTransition = null;
        }
    }

    private void setInitialUiState() {
        startSessionButton.setVisible(true);
        startSessionButton.setManaged(true);
        wordCountSpinner.setDisable(false);
        nextButton.setVisible(false);
        nextButton.setManaged(false);
        endSessionButton.setVisible(false);
        endSessionButton.setManaged(false);
        choiceBox.setVisible(false);
        choiceBox.setManaged(false);
        challengeBox.setVisible(false);
        challengeBox.setManaged(false);
        continueButton.setVisible(false);
        continueButton.setManaged(false);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        modeDescriptionLabel.setText("Selecciona un modo y empieza la practica.");
    }

    private void setInteractionDisabled(boolean disabled) {
        submitAnswerButton.setDisable(disabled);
        challengeInput.setDisable(disabled);
        choiceButton1.setDisable(disabled);
        choiceButton2.setDisable(disabled);
        choiceButton3.setDisable(disabled);
        choiceButton4.setDisable(disabled);
        nextButton.setDisable(disabled);
        continueButton.setDisable(disabled);
        modeCombo.setDisable(disabled);
    }

    private void configureWordCountSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        MIN_SESSION_WORD_COUNT,
                        MAX_SESSION_WORD_COUNT,
                        DEFAULT_SESSION_WORD_COUNT);
        wordCountSpinner.setValueFactory(valueFactory);
        wordCountSpinner.setEditable(true);

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            return text.matches("\\d{0,3}") ? change : null;
        };
        wordCountSpinner.getEditor().setTextFormatter(new TextFormatter<>(filter));

        wordCountSpinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                applySpinnerEditorValue();
            }
        });
    }

    private int getRequestedWordCount() {
        applySpinnerEditorValue();
        Integer value = wordCountSpinner.getValue();
        if (value == null) {
            return DEFAULT_SESSION_WORD_COUNT;
        }
        return Math.max(MIN_SESSION_WORD_COUNT, Math.min(value, MAX_SESSION_WORD_COUNT));
    }

    private void applySpinnerEditorValue() {
        String text = wordCountSpinner.getEditor().getText();
        if (text == null || text.isBlank()) {
            wordCountSpinner.getValueFactory().setValue(DEFAULT_SESSION_WORD_COUNT);
            return;
        }

        try {
            int parsed = Integer.parseInt(text.trim());
            int clamped = Math.max(MIN_SESSION_WORD_COUNT, Math.min(parsed, MAX_SESSION_WORD_COUNT));
            wordCountSpinner.getValueFactory().setValue(clamped);
            wordCountSpinner.getEditor().setText(String.valueOf(clamped));
        } catch (NumberFormatException ex) {
            Integer current = wordCountSpinner.getValue();
            int fallback = current != null ? current : DEFAULT_SESSION_WORD_COUNT;
            wordCountSpinner.getValueFactory().setValue(fallback);
            wordCountSpinner.getEditor().setText(String.valueOf(fallback));
        }
    }

    private void highlightChoiceResult(Button selected, boolean correct) {
        clearChoiceStyles();
        if (selected != null) {
            selected.getStyleClass().add(correct ? "practice-option-correct" : "practice-option-error");
        }
        if (!correct) {
            Button right = findCorrectChoiceButton();
            if (right != null && right != selected) {
                right.getStyleClass().add("practice-option-correct");
            }
        }
    }

    private Button findCorrectChoiceButton() {
        List<Button> buttons = List.of(choiceButton1, choiceButton2, choiceButton3, choiceButton4);
        String target = normalize(currentWord != null ? currentWord.getTerm() : "");
        for (Button b : buttons) {
            if (b != null && normalize(b.getText()).equals(target)) {
                return b;
            }
        }
        return null;
    }

    private void clearChoiceStyles() {
        List<Button> buttons = List.of(choiceButton1, choiceButton2, choiceButton3, choiceButton4);
        for (Button b : buttons) {
            if (b == null) {
                continue;
            }
            b.getStyleClass().remove("practice-option-correct");
            b.getStyleClass().remove("practice-option-error");
        }
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("practice-feedback-success", "practice-feedback-error");
        feedbackLabel.getStyleClass().add(success ? "practice-feedback-success" : "practice-feedback-error");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void updateProgressDisplay() {
        if (sessionWords.isEmpty()) {
            progressLabel.setText("Progress: Ready to start");
            progressBar.setProgress(0.0);
            return;
        }

        int total = sessionWords.size();
        int done = Math.min(currentIndex, total);
        int remaining = Math.max(0, total - done);
        progressBar.setProgress(total == 0 ? 0.0 : (double) done / total);
        progressLabel.setText(String.format("Progress: %d/%d words (%d remaining)", done, total, remaining));
    }

    private void updateSessionStatsDisplay() {
        if (sessionWords.isEmpty()) {
            sessionStatsLabel.setText("Session Stats: Not started");
            return;
        }
        int incorrect = Math.max(0, wordsReviewed - correctAnswers);
        sessionStatsLabel.setText(String.format("Session Stats: %d correct, %d incorrect", correctAnswers, incorrect));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.trim().toLowerCase();
        String withoutAccents = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
