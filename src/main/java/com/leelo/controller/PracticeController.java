package com.leelo.controller;

import com.leelo.model.Word;
import com.leelo.service.WordService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PracticeController {
    private enum PracticeMode {
        FLUIDEZ("Modo Fluidez"),
        DESAFIO("Modo Desafio (Cloze)"),
        RELAJADO("Modo Repaso Relajado");

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
    @FXML private Label progressLabel;
    @FXML private Label sessionStatsLabel;
    @FXML private Button showButton;
    @FXML private Button nextButton;
    @FXML private Button startSessionButton;
    @FXML private Button endSessionButton;
    @FXML private Button correctButton;
    @FXML private Button incorrectButton;
    @FXML private ProgressBar progressBar;
    @FXML private HBox reviewButtonsBox;
    @FXML private ComboBox<PracticeMode> modeCombo;
    @FXML private VBox challengeBox;
    @FXML private TextField challengeInput;
    @FXML private Button submitAnswerButton;
    @FXML private VBox choiceBox;
    @FXML private Button choiceButton1;
    @FXML private Button choiceButton2;
    @FXML private Button choiceButton3;
    @FXML private Button choiceButton4;
    @FXML private SideMenuController menuController;

    private final WordService wordService = new WordService();

    private List<Word> sessionWords = new ArrayList<>();
    private Word currentWord;
    private boolean answerShown;
    private int currentIndex;
    private int wordsReviewed;
    private int correctAnswers;

    @FXML
    public void initialize() {
        if (menuController != null) {
            menuController.setActiveSection("practice");
        }

        modeCombo.setItems(FXCollections.observableArrayList(PracticeMode.values()));
        modeCombo.setValue(PracticeMode.FLUIDEZ);
        modeCombo.valueProperty().addListener((obs, oldV, newV) -> refreshModeView());

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
        answerShown = false;

        if (sessionWords.isEmpty()) {
            wordLabel.setText("No hay palabras para practicar");
            translationLabel.setText("Agrega palabras con significado para empezar.");
            translationLabel.setVisible(true);
            translationLabel.setManaged(true);
            return;
        }

        startSessionButton.setVisible(false);
        startSessionButton.setManaged(false);
        nextButton.setVisible(true);
        nextButton.setManaged(true);
        endSessionButton.setVisible(true);
        endSessionButton.setManaged(true);

        showCurrentWord();
    }

    @FXML
    public void showAnswer() {
        if (currentWord == null || answerShown) {
            return;
        }

        answerShown = true;
        translationLabel.setText(currentWord.getTranslation());
        translationLabel.setVisible(true);
        translationLabel.setManaged(true);

        if (currentWord.getPronunciation() != null && !currentWord.getPronunciation().isBlank()) {
            pronunciationLabel.setText("[" + currentWord.getPronunciation() + "]");
            pronunciationLabel.setVisible(true);
            pronunciationLabel.setManaged(true);
        }

        reviewButtonsBox.setVisible(true);
        reviewButtonsBox.setManaged(true);
        showButton.setVisible(false);
        showButton.setManaged(false);
    }

    @FXML
    public void markWordCorrect() {
        if (currentWord == null || !answerShown) {
            return;
        }
        handleAnswerResult(true);
    }

    @FXML
    public void markWordIncorrect() {
        if (currentWord == null || !answerShown) {
            return;
        }
        handleAnswerResult(false);
    }

    @FXML
    public void submitChallengeAnswer() {
        if (currentWord == null) {
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
        if (currentWord == null || button.getText() == null) {
            return;
        }
        boolean correct = normalize(button.getText()).equals(normalize(currentWord.getTerm()));
        handleAnswerResult(correct);
    }

    @FXML
    public void skipWord() {
        if (currentWord == null) {
            return;
        }
        currentIndex++;
        showCurrentWord();
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

        resetSessionState();
        setInitialUiState();
        updateProgressDisplay();
        updateSessionStatsDisplay();
    }

    private void handleAnswerResult(boolean correct) {
        wordsReviewed++;
        if (correct) {
            correctAnswers++;
            translationLabel.setText("Correcto: " + currentWord.getTranslation());
        } else {
            translationLabel.setText("Incorrecto. Respuesta: " + currentWord.getTerm() + " = " + currentWord.getTranslation());
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

        currentIndex++;
        showCurrentWord();
    }

    private void showCurrentWord() {
        if (currentIndex >= sessionWords.size()) {
            endStudySession();
            return;
        }

        currentWord = sessionWords.get(currentIndex);
        answerShown = false;

        pronunciationLabel.setVisible(false);
        pronunciationLabel.setManaged(false);
        reviewButtonsBox.setVisible(false);
        reviewButtonsBox.setManaged(false);
        choiceBox.setVisible(false);
        choiceBox.setManaged(false);
        challengeBox.setVisible(false);
        challengeBox.setManaged(false);
        challengeInput.clear();

        updateProgressDisplay();
        updateSessionStatsDisplay();
        refreshModeView();
    }

    private void refreshModeView() {
        PracticeMode mode = modeCombo.getValue() != null ? modeCombo.getValue() : PracticeMode.FLUIDEZ;
        if (currentWord == null) {
            return;
        }

        switch (mode) {
            case FLUIDEZ:
                wordLabel.setText(currentWord.getTerm());
                translationLabel.setText("Piensa el significado y luego presiona 'Show Answer'.");
                translationLabel.setVisible(true);
                translationLabel.setManaged(true);
                showButton.setVisible(true);
                showButton.setManaged(true);
                break;

            case DESAFIO:
                wordLabel.setText("Completa la palabra para:");
                translationLabel.setText(currentWord.getTranslation());
                translationLabel.setVisible(true);
                translationLabel.setManaged(true);
                challengeBox.setVisible(true);
                challengeBox.setManaged(true);
                showButton.setVisible(false);
                showButton.setManaged(false);
                break;

            case RELAJADO:
                wordLabel.setText("Selecciona la palabra correcta para:");
                translationLabel.setText(currentWord.getTranslation());
                translationLabel.setVisible(true);
                translationLabel.setManaged(true);
                buildMultipleChoiceOptions();
                choiceBox.setVisible(true);
                choiceBox.setManaged(true);
                showButton.setVisible(false);
                showButton.setManaged(false);
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
        int maxSessionWords = Math.min(words.size(), 30);
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
        answerShown = false;
    }

    private void setInitialUiState() {
        startSessionButton.setVisible(true);
        startSessionButton.setManaged(true);
        showButton.setVisible(false);
        showButton.setManaged(false);
        nextButton.setVisible(false);
        nextButton.setManaged(false);
        endSessionButton.setVisible(false);
        endSessionButton.setManaged(false);
        reviewButtonsBox.setVisible(false);
        reviewButtonsBox.setManaged(false);
        choiceBox.setVisible(false);
        choiceBox.setManaged(false);
        challengeBox.setVisible(false);
        challengeBox.setManaged(false);
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
