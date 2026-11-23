package com.leelo.controller;

import com.leelo.model.Book;
import com.leelo.model.BookPage;
import com.leelo.model.BookStats;
import com.leelo.model.Language;
import com.leelo.model.Term;
import com.leelo.model.TermStatus;
import com.leelo.model.Theme;
import com.leelo.model.AiDefinition;
import com.leelo.render.RenderedParagraph;
import com.leelo.render.RenderedSentence;
import com.leelo.render.TextItem;
import com.leelo.service.BookmarkService;
import com.leelo.service.BookService;
import com.leelo.service.HotkeyService;
import com.leelo.service.ImageSearchService;
import com.leelo.service.LanguageService;
import com.leelo.service.OllamaService;
import com.leelo.service.ReadingService;
import com.leelo.service.SettingService;
import com.leelo.service.StatisticsService;
import com.leelo.service.TermService;
import com.leelo.service.ThemeService;
import com.leelo.util.TextTokens;
import com.leelo.view.InlineText;
import com.leelo.view.StatusConverter;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The reading screen.
 *
 * <p>The page is rendered as a flow of inline texts: the words that are terms
 * carry the colour of their status and can be clicked to open the details of
 * the term.
 */
public class ReadingController {

    @FXML
    private VBox paragraphsBox;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Label titleLabel;
    @FXML
    private Label languageLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private HBox headerBar;
    @FXML
    private HBox footerBar;
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button markReadButton;
    @FXML
    private Button bookmarkButton;
    @FXML
    private Button fontIncreaseButton;
    @FXML
    private Button fontDecreaseButton;
    @FXML
    private Button focusButton;
    @FXML
    private Button exitFocusButton;
    @FXML
    private HBox focusPager;
    @FXML
    private Button focusPrevPageButton;
    @FXML
    private Label focusPageLabel;
    @FXML
    private Button focusNextPageButton;
    @FXML
    private Button highlightButton;
    @FXML
    private VBox termPanel;
    @FXML
    private Label termTitleLabel;
    @FXML
    private Label pronunciationLabel;
    @FXML
    private TextArea translationArea;
    @FXML
    private Button aiTranslateButton;
    @FXML
    private Label aiStatusLabel;
    @FXML
    private TextField parentsField;
    @FXML
    private TextField tagsField;
    @FXML
    private ImageView termImageView;
    @FXML
    private FlowPane statusButtons;
    @FXML
    private FlowPane statusLegend;
    @FXML
    private Button saveTermButton;
    @FXML
    private Button deleteTermButton;
    @FXML
    private Button closeTermButton;
    @FXML
    private SideMenuController menuController;

    private final ReadingService readingService = new ReadingService();
    private final BookService bookService = new BookService();
    private final TermService termService = new TermService();
    private final LanguageService languageService = new LanguageService();
    private final BookmarkService bookmarkService = new BookmarkService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final SettingService settingService = new SettingService();
    private final ThemeService themeService = new ThemeService(settingService);
    private final HotkeyService hotkeyService = new HotkeyService(settingService);
    private final ImageSearchService imageSearchService = new ImageSearchService();
    private final OllamaService ollamaService = new OllamaService();

    private final EventHandler<KeyEvent> keyFilter = this::onKeyPressed;
    private final Map<InlineText, TextItem> itemsByNode = new IdentityHashMap<>();
    private final List<InlineText> wordNodes = new ArrayList<>();

    private Book book;
    private Language language;
    private List<BookPage> pages = new ArrayList<>();
    private int currentPageIndex;
    private ReadingService.PageContent currentContent;
    private Term selectedTerm;
    private TextItem selectedItem;
    private InlineText hoveredNode;
    private boolean focusMode;
    private double fontSize = 25;

    @FXML
    public void initialize() {
        fontSize = settingService.getInt(SettingService.FONT_SIZE);
        prevPageButton.setOnAction(event -> goToPage(currentPageIndex - 1));
        nextPageButton.setOnAction(event -> goToPage(currentPageIndex + 1));
        markReadButton.setOnAction(event -> markPageRead());
        bookmarkButton.setOnAction(event -> addBookmark());
        fontIncreaseButton.setOnAction(event -> changeFontSize(2));
        fontDecreaseButton.setOnAction(event -> changeFontSize(-2));
        focusButton.setOnAction(event -> toggleFocusMode());
        exitFocusButton.setOnAction(event -> setFocusMode(false));
        focusPrevPageButton.setOnAction(event -> goToPage(currentPageIndex - 1));
        focusNextPageButton.setOnAction(event -> goToPage(currentPageIndex + 1));
        highlightButton.setOnAction(event -> toggleHighlights());
        saveTermButton.setOnAction(event -> saveSelectedTerm());
        deleteTermButton.setOnAction(event -> deleteSelectedTerm());
        closeTermButton.setOnAction(event -> hideTermPanel());
        aiTranslateButton.setOnAction(event -> translateSelectedTermWithAi());

        buildStatusButtons();
        buildStatusLegend();
        updateHighlightButton();
        attachToScene(paragraphsBox);
    }

    /**
     * Opens a book in the reading screen.
     */
    public void setBook(Book selectedBook) {
        this.book = selectedBook;
        this.language = languageService.getById(selectedBook.getLanguageId());
        this.pages = bookService.getPages(selectedBook.getId());
        if (pages.isEmpty()) {
            return;
        }
        titleLabel.setText(selectedBook.getTitle());
        languageLabel.setText(language == null ? "" : language.getName());

        int currentPageId = selectedBook.getCurrentPageId();
        currentPageIndex = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getId() == currentPageId) {
                currentPageIndex = i;
                break;
            }
        }
        renderPage(true);
    }

    /** Renders the current page. */
    private void renderPage(boolean trackOpen) {
        if (book == null || language == null || pages.isEmpty()) {
            return;
        }
        BookPage page = pages.get(currentPageIndex);
        ReadingService.PageContent content = readingService.getPageContent(book, page, language, trackOpen);
        currentContent = content;

        Theme theme = themeService.getCurrentTheme();
        boolean highlighted = themeService.isHighlightingEnabled();
        applyThemeSurface(theme);

        paragraphsBox.getChildren().clear();
        itemsByNode.clear();
        wordNodes.clear();

        for (RenderedParagraph paragraph : content.paragraphs()) {
            TextFlow flow = new TextFlow();
            flow.setLineSpacing(fontSize * 0.35);
            for (RenderedSentence sentence : paragraph.getSentences()) {
                for (TextItem item : sentence.getItems()) {
                    flow.getChildren().add(createNode(item, theme, highlighted));
                }
            }
            paragraphsBox.getChildren().add(flow);
        }

        updatePagers();
        markReadButton.setDisable(page.isRead());
        updateProgressLabel();
    }

    /** Keeps the header and the floating pager in sync. */
    private void updatePagers() {
        String position = (currentPageIndex + 1) + " / " + pages.size();
        pageLabel.setText(position);
        focusPageLabel.setText(position);
        boolean firstPage = currentPageIndex <= 0;
        boolean lastPage = currentPageIndex >= pages.size() - 1;
        prevPageButton.setDisable(firstPage);
        focusPrevPageButton.setDisable(firstPage);
        nextPageButton.setDisable(lastPage);
        focusNextPageButton.setDisable(lastPage);
    }

    /** Updates the label showing how much of the book was read. */
    private void updateProgressLabel() {
        long readCount = pages.stream().filter(BookPage::isRead).count();
        BookStats stats = statisticsService.getBookStats(book, language);
        progressLabel.setText(readCount + "/" + pages.size() + " páginas  ·  "
                + stats.getUnknownPercent() + "% por aprender");
    }

    /** Paints the reading surface with the background of the current theme. */
    private void applyThemeSurface(Theme theme) {
        String background = toCssColor(theme.getBackground());
        scrollPane.setStyle("-fx-background-color: " + background + "; -fx-background: " + background + ";");
        paragraphsBox.setStyle("-fx-background-color: " + background + ";");
    }

    /** Creates the node rendering a single text item. */
    private Node createNode(TextItem item, Theme theme, boolean highlighted) {
        TermStatus status = item.getTerm() == null
                ? TermStatus.UNKNOWN : item.getTerm().getStatus();
        Color background = highlighted && item.isWord() && status.isHighlighted()
                ? theme.getStatusColor(status)
                : null;
        Color fill = theme.getForeground();

        InlineText node = new InlineText(item.getPlainDisplayText(),
                Font.font(fontSize), fill, background);
        if (item.isWord()) {
            itemsByNode.put(node, item);
            wordNodes.add(node);
            node.setOnMouseClicked(event -> onWordClicked(event, item));
            node.setOnMouseEntered(event -> setHovered(node, true));
            node.setOnMouseExited(event -> setHovered(node, false));
        }
        return node;
    }

    /** Handles the click on a word. */
    private void onWordClicked(javafx.scene.input.MouseEvent event, TextItem item) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        event.consume();
        selectItem(item);
    }

    /** Selects a text item and shows its details. */
    private void selectItem(TextItem item) {
        selectedItem = item;
        selectedTerm = item.getTerm();
        if (selectedTerm == null) {
            return;
        }
        termTitleLabel.setText(TextTokens.strip(selectedTerm.getText()));
        pronunciationLabel.setText(selectedTerm.getRomanization());
        translationArea.setText(selectedTerm.getTranslation());
        parentsField.setText(joinParents(selectedTerm));
        tagsField.setText(joinTags(selectedTerm));
        showImage(selectedTerm);
        resetAiStatus();
        termPanel.setVisible(true);
        termPanel.setManaged(true);
        updateStatusButtons();

        if (settingService.getBoolean(SettingService.OLLAMA_AUTO_TRANSLATE)
                && (selectedTerm.getTranslation() == null || selectedTerm.getTranslation().isBlank())) {
            translateSelectedTermWithAi();
        }
    }

    /** Shows the image of a term, when it has one. */
    private void showImage(Term term) {
        String source = term.getImage() == null ? "" : term.getImage().getSource();
        if (source.isBlank()) {
            termImageView.setImage(null);
            termImageView.setVisible(false);
            termImageView.setManaged(false);
            return;
        }
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return imageSearchService.downloadImage(source);
            }
        };
        task.setOnSucceeded(event -> {
            termImageView.setImage(task.getValue());
            termImageView.setVisible(true);
            termImageView.setManaged(true);
        });
        task.setOnFailed(event -> {
            termImageView.setImage(null);
            termImageView.setVisible(false);
            termImageView.setManaged(false);
        });
        Thread worker = new Thread(task, "leelo-reading-term-image");
        worker.setDaemon(true);
        worker.start();
    }

    /** Hides the details of the term. */
    private void hideTermPanel() {
        resetAiStatus();
        termPanel.setVisible(false);
        termPanel.setManaged(false);
        selectedTerm = null;
        selectedItem = null;
    }

    /** Clears AI status feedback and re-enables the AI button. */
    private void resetAiStatus() {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("");
            aiStatusLabel.setVisible(false);
            aiStatusLabel.setManaged(false);
        }
        if (aiTranslateButton != null) {
            aiTranslateButton.setDisable(false);
        }
    }

    /** Queries Ollama to translate, define, and give an example for the selected term. */
    public void translateSelectedTermWithAi() {
        if (selectedTerm == null) {
            return;
        }
        final Term termToTranslate = selectedTerm;
        String termText = TextTokens.strip(termToTranslate.getText());
        if (termText.isBlank()) {
            return;
        }

        String contextSentence = textForScope(1);

        aiTranslateButton.setDisable(true);
        aiStatusLabel.setText("Consultando Ollama (" + ollamaService.getModel() + ")...");
        aiStatusLabel.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 11px;");
        aiStatusLabel.setVisible(true);
        aiStatusLabel.setManaged(true);

        Task<AiDefinition> task = new Task<>() {
            @Override
            protected AiDefinition call() throws Exception {
                return ollamaService.generateDefinition(termText, contextSentence);
            }
        };

        task.setOnSucceeded(event -> {
            aiTranslateButton.setDisable(false);
            AiDefinition definition = task.getValue();
            if (definition != null) {
                String formatted = definition.formatForTranslationArea();
                if (selectedTerm == termToTranslate) {
                    translationArea.setText(formatted);
                    aiStatusLabel.setText("✓ Traducido con Ollama");
                    aiStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
                }
                termToTranslate.setTranslation(formatted);
                termService.save(termToTranslate);
            }
        });

        task.setOnFailed(event -> {
            aiTranslateButton.setDisable(false);
            Throwable error = task.getException();
            String msg = error != null && error.getMessage() != null
                    ? error.getMessage()
                    : "Error al consultar Ollama";
            aiStatusLabel.setText(msg);
            aiStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
        });

        Thread worker = new Thread(task, "leelo-ollama-translate");
        worker.setDaemon(true);
        worker.start();
    }

    /** Builds the buttons that set the status of the selected term. */
    private void buildStatusButtons() {
        statusButtons.getChildren().clear();
        for (TermStatus status : TermStatus.values()) {
            if (!status.isAssignable()) {
                continue;
            }
            Button button = new Button(StatusConverter.labelFor(status));
            button.getStyleClass().add("status-button");
            button.setOnAction(event -> applyStatus(status));
            statusButtons.getChildren().add(button);
        }
    }

    /** Builds the legend showing the colour of every status. */
    private void buildStatusLegend() {
        statusLegend.getChildren().clear();
        Theme theme = themeService.getCurrentTheme();
        for (TermStatus status : TermStatus.values()) {
            Region dot = new Region();
            dot.getStyleClass().add("status-dot");
            String color = status.isHighlighted()
                    ? toCssColor(theme.getStatusColor(status)) : "transparent";
            dot.setStyle("-fx-background-color: " + color + ";");
            javafx.scene.control.Tooltip.install(dot,
                    new javafx.scene.control.Tooltip(StatusConverter.labelFor(status)));
            statusLegend.getChildren().add(dot);
        }
    }

    /** Highlights the button of the status of the selected term. */
    private void updateStatusButtons() {
        int index = 0;
        for (Node node : statusButtons.getChildren()) {
            if (!(node instanceof Button button)) {
                continue;
            }
            TermStatus status = assignableStatusAt(index++);
            boolean active = selectedTerm != null && status != null
                    && selectedTerm.getStatus() == status;
            if (active) {
                Color color = themeService.getCurrentTheme().getStatusColor(status);
                String textColor = color.getBrightness() > 0.62 ? "#1b1b18" : "#ffffff";
                button.setStyle("-fx-background-color: " + toCssColor(color)
                        + "; -fx-text-fill: " + textColor + "; -fx-border-color: transparent;");
            } else {
                button.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #5e5e56;"
                        + " -fx-border-color: #d6d5cc;");
            }
        }
    }

    /** Returns the n-th status that can be assigned to a term. */
    private TermStatus assignableStatusAt(int index) {
        int current = 0;
        for (TermStatus status : TermStatus.values()) {
            if (!status.isAssignable()) {
                continue;
            }
            if (current == index) {
                return status;
            }
            current++;
        }
        return null;
    }

    /** Applies a status to the selected term. */
    private void applyStatus(TermStatus status) {
        if (selectedTerm == null) {
            return;
        }
        termService.setStatus(selectedTerm.getId(), status);
        selectedTerm.setStatus(status);
        renderPage(false);
        updateStatusButtons();
    }

    /** Saves the changes made in the details of the term. */
    private void saveSelectedTerm() {
        if (selectedTerm == null) {
            return;
        }
        selectedTerm.setTranslation(translationArea.getText());
        selectedTerm.setTags(termService.resolveTags(splitList(tagsField.getText())));
        selectedTerm.setParents(resolveParents(parentsField.getText()));
        termService.save(selectedTerm);
        renderPage(false);
    }

    /** Deletes the selected term. */
    private void deleteSelectedTerm() {
        if (selectedTerm == null) {
            return;
        }
        termService.delete(selectedTerm.getId());
        hideTermPanel();
        renderPage(false);
    }

    /** Turns the parent names into the terms they point at. */
    private List<Term> resolveParents(String text) {
        List<Term> parents = new ArrayList<>();
        for (String name : splitList(text)) {
            Term parent = termService.findOrCreate(language, name);
            if (parent.getId() > 0 && parent.getId() != selectedTerm.getId()) {
                parents.add(parent);
            }
        }
        return parents;
    }

    /** Joins the names of the parents of a term. */
    private String joinParents(Term term) {
        List<String> names = new ArrayList<>();
        for (Term parent : term.getParents()) {
            names.add(TextTokens.strip(parent.getText()));
        }
        return String.join(", ", names);
    }

    /** Joins the names of the tags of a term. */
    private String joinTags(Term term) {
        List<String> names = new ArrayList<>();
        for (var tag : term.getTags()) {
            names.add(tag.getText());
        }
        return String.join(", ", names);
    }

    /** Splits a comma separated list. */
    private List<String> splitList(String text) {
        List<String> items = new ArrayList<>();
        if (text == null) {
            return items;
        }
        for (String item : text.split(",")) {
            if (!item.isBlank()) {
                items.add(item.trim());
            }
        }
        return items;
    }

    /** Goes to another page of the book. */
    private void goToPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        currentPageIndex = index;
        hideTermPanel();
        renderPage(true);
        scrollPane.setVvalue(0);
    }

    /** Marks the current page as read and moves to the next one. */
    private void markPageRead() {
        if (book == null || pages.isEmpty()) {
            return;
        }
        BookPage page = pages.get(currentPageIndex);
        readingService.markPageRead(book, page, language);
        page.setReadDate(java.time.Instant.now());
        if (currentPageIndex < pages.size() - 1) {
            goToPage(currentPageIndex + 1);
        } else {
            renderPage(false);
        }
    }

    /** Adds a bookmark to the current page. */
    private void addBookmark() {
        if (book == null || pages.isEmpty()) {
            return;
        }
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Bookmark");
        dialog.setHeaderText("Give the bookmark a title");
        java.util.Optional<String> title = dialog.showAndWait();
        title.ifPresent(value -> bookmarkService.add(pages.get(currentPageIndex).getId(), value));
    }

    /** Changes the font size of the page. */
    private void changeFontSize(double delta) {
        fontSize = Math.max(12, Math.min(48, fontSize + delta));
        settingService.setInt(SettingService.FONT_SIZE, (int) fontSize);
        renderPage(false);
    }

    /** Turns the highlights of the terms on or off. */
    private void toggleHighlights() {
        themeService.toggleHighlighting();
        updateHighlightButton();
        renderPage(false);
    }

    /** Updates the label of the button that toggles the highlights. */
    private void updateHighlightButton() {
        boolean enabled = themeService.isHighlightingEnabled();
        highlightButton.setText("Colores");
        highlightButton.getStyleClass().remove("toolbar-toggle-active");
        if (enabled) {
            highlightButton.getStyleClass().add("toolbar-toggle-active");
        }
    }

    /** Hides everything but the text so that it can be read without distractions. */
    private void toggleFocusMode() {
        setFocusMode(!focusMode);
    }

    /** Enters or leaves the focus mode. */
    private void setFocusMode(boolean enabled) {
        focusMode = enabled;
        menuController.setVisible(!enabled);
        headerBar.setVisible(!enabled);
        headerBar.setManaged(!enabled);
        footerBar.setVisible(!enabled);
        footerBar.setManaged(!enabled);
        exitFocusButton.setVisible(enabled);
        exitFocusButton.setManaged(enabled);
        focusPager.setVisible(enabled);
        focusPager.setManaged(enabled);
    }

    /** Marks the word under the mouse with an underline. */
    private void setHovered(InlineText node, boolean hovered) {
        if (hovered) {
            hoveredNode = node;
            node.setUnderlined(true);
        } else if (hoveredNode == node) {
            node.setUnderlined(false);
            hoveredNode = null;
        }
    }

    /** Moves the selection to the next or the previous word. */
    private void moveSelection(int delta) {
        if (wordNodes.isEmpty()) {
            return;
        }
        int index = wordNodes.indexOf(selectedNode());
        int next = Math.max(0, Math.min(wordNodes.size() - 1, index + delta));
        InlineText node = wordNodes.get(next);
        TextItem item = itemsByNode.get(node);
        if (item != null) {
            selectItem(item);
            node.requestFocus();
        }
    }

    /** Returns the node of the selected word. */
    private InlineText selectedNode() {
        for (Map.Entry<InlineText, TextItem> entry : itemsByNode.entrySet()) {
            if (entry.getValue() == selectedItem) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Handles the keyboard shortcuts of the reading screen. */
    private void onKeyPressed(KeyEvent event) {
        if (focusMode && event.getCode() == KeyCode.ESCAPE) {
            setFocusMode(false);
            event.consume();
            return;
        }
        if (focusMode && event.getCode() == KeyCode.PAGE_UP && currentPageIndex > 0) {
            goToPage(currentPageIndex - 1);
            event.consume();
            return;
        }
        if (focusMode && event.getCode() == KeyCode.PAGE_DOWN && currentPageIndex < pages.size() - 1) {
            goToPage(currentPageIndex + 1);
            event.consume();
            return;
        }
        if (event.getTarget() instanceof TextInputControl) {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                saveSelectedTerm();
                event.consume();
            }
            return;
        }
        HotkeyService.Action action = hotkeyService.match(event);
        if (action == null) {
            return;
        }
        if (executeAction(action)) {
            event.consume();
        }
    }

    /** Runs the action bound to a shortcut. */
    private boolean executeAction(HotkeyService.Action action) {
        switch (action) {
            case NEXT_WORD:
                moveSelection(1);
                return true;
            case PREV_WORD:
                moveSelection(-1);
                return true;
            case STATUS_1:
                applyStatus(TermStatus.NEW_1);
                return true;
            case STATUS_2:
                applyStatus(TermStatus.NEW_2);
                return true;
            case STATUS_3:
                applyStatus(TermStatus.LEARNING_3);
                return true;
            case STATUS_4:
                applyStatus(TermStatus.LEARNING_4);
                return true;
            case STATUS_5:
                applyStatus(TermStatus.LEARNED);
                return true;
            case STATUS_IGNORE:
                applyStatus(TermStatus.IGNORED);
                return true;
            case STATUS_WELL_KNOWN:
                applyStatus(TermStatus.WELL_KNOWN);
                return true;
            case STATUS_UP:
                stepStatus(1);
                return true;
            case STATUS_DOWN:
                stepStatus(-1);
                return true;
            case DELETE_TERM:
                deleteSelectedTerm();
                return true;
            case PREVIOUS_PAGE:
                goToPage(currentPageIndex - 1);
                return true;
            case NEXT_PAGE:
                goToPage(currentPageIndex + 1);
                return true;
            case MARK_READ:
                markPageRead();
                return true;
            case MARK_READ_WELL_KNOWN:
                markUnknownsAsKnown();
                return true;
            case TOGGLE_HIGHLIGHT:
                toggleHighlights();
                return true;
            case TOGGLE_FOCUS:
                toggleFocusMode();
                return true;
            case BOOKMARK:
                addBookmark();
                return true;
            case TRANSLATE_SENTENCE:
                translateSelectedTermWithAi();
                return true;
            case COPY_SENTENCE:
                copySelection(1);
                return true;
            case COPY_PARAGRAPH:
                copySelection(2);
                return true;
            case COPY_PAGE:
                copySelection(3);
                return true;
            case SAVE_TERM:
                saveSelectedTerm();
                return true;
            default:
                return false;
        }
    }

    /** Moves the status of the selected term up or down. */
    private void stepStatus(int delta) {
        if (selectedTerm == null) {
            return;
        }
        TermStatus next = delta > 0 ? selectedTerm.getStatus().stepUp() : selectedTerm.getStatus().stepDown();
        if (next != null) {
            applyStatus(next);
        }
    }

    /** Sets the words of the page that are still unknown to "Well Known". */
    private void markUnknownsAsKnown() {
        if (book == null || pages.isEmpty()) {
            return;
        }
        readingService.setUnknownsToKnown(pages.get(currentPageIndex), language);
        markPageRead();
    }

    /** Copies the sentence, the paragraph or the page to the clipboard. */
    private void copySelection(int scope) {
        if (selectedItem == null) {
            return;
        }
        String text = textForScope(scope);
        if (text.isEmpty()) {
            return;
        }
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    }

    /** Returns the text of the sentence, the paragraph or the page. */
    private String textForScope(int scope) {
        if (scope == 3) {
            return pages.isEmpty() ? "" : pages.get(currentPageIndex).getContent();
        }
        StringBuilder builder = new StringBuilder();
        for (RenderedParagraph paragraph : currentParagraphs()) {
            for (RenderedSentence sentence : paragraph.getSentences()) {
                boolean containsSelection = false;
                for (TextItem item : sentence.getItems()) {
                    if (item == selectedItem) {
                        containsSelection = true;
                        break;
                    }
                }
                if (!containsSelection) {
                    continue;
                }
                if (scope == 1) {
                    builder.append(sentence.getPlainText());
                } else {
                    builder.append(paragraph.getPlainText());
                }
                return builder.toString();
            }
        }
        return builder.toString();
    }

    /** Returns the paragraphs of the page currently displayed. */
    private List<RenderedParagraph> currentParagraphs() {
        if (currentContent == null) {
            return List.of();
        }
        return currentContent.paragraphs();
    }

    /** Converts a colour into its CSS representation. */
    private String toCssColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    /** Attaches the keyboard handler to the scene of the screen. */
    private void attachToScene(Node node) {
        ChangeListener<Scene> listener = (observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            }
        };
        node.sceneProperty().addListener(listener);
    }
}
