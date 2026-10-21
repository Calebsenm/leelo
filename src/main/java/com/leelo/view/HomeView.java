package com.leelo.view;

import com.leelo.model.Book;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The home screen: the book that is being read and the library.
 */
public class HomeView extends VBox {

    private final Label bookTitleLabel;
    private final Label wordCountLabel;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label streakLabel;
    private final Label streakValueLabel;
    private final Label booksCountLabel;
    private final Label pagesCountLabel;
    private final Label libraryCountLabel;
    private final Label coverLetterLabel;
    private final ListView<Book> booksList;
    private final Button addButton;

    private Runnable openBookHandler = () -> { };
    private Runnable addBookHandler = () -> { };
    private Book selectedBook;
    private boolean hasBook;

    public HomeView() {
        getStyleClass().add("home-root");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(0);

        VBox content = new VBox(22);
        content.getStyleClass().add("home-content");
        content.setMaxWidth(1180);
        content.setFillWidth(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        /* Header -------------------------------------------------------- */
        VBox heading = new VBox(3);
        Label title = new Label("Biblioteca");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Tu espacio de lectura y vocabulario.");
        subtitle.getStyleClass().add("page-subtitle");
        heading.getChildren().addAll(title, subtitle);

        addButton = new Button("+ Nuevo texto");
        addButton.getStyleClass().addAll("btn", "btn-primary");
        addButton.setOnAction(event -> addBookHandler.run());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(16, heading, headerSpacer, addButton);
        header.getStyleClass().add("home-header");
        header.setAlignment(Pos.CENTER_LEFT);

        /* Continue reading --------------------------------------------- */
        coverLetterLabel = new Label("L");
        coverLetterLabel.getStyleClass().add("book-cover-letter");
        StackPane cover = new StackPane(coverLetterLabel);
        cover.getStyleClass().add("book-cover");

        Label continueEyebrow = new Label("CONTINUAR LEYENDO");
        continueEyebrow.getStyleClass().add("eyebrow");

        bookTitleLabel = new Label("Sin libro");
        bookTitleLabel.getStyleClass().add("continue-title");

        wordCountLabel = new Label("0 palabras");
        wordCountLabel.getStyleClass().add("continue-meta");

        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefWidth(320);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        progressLabel = new Label("0%");
        progressLabel.getStyleClass().add("progress-value");

        HBox progressBox = new HBox(12, progressBar, progressLabel);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        VBox continueInfo = new VBox(4, continueEyebrow, bookTitleLabel, wordCountLabel, progressBox);
        HBox.setHgrow(continueInfo, Priority.ALWAYS);

        Label continueHint = new Label("Continuar  →");
        continueHint.getStyleClass().add("continue-hint");

        HBox continueCard = new HBox(20, cover, continueInfo, continueHint);
        continueCard.getStyleClass().addAll("card", "continue-card");
        continueCard.setAlignment(Pos.CENTER_LEFT);
        continueCard.setOnMouseClicked(event -> {
            if (hasBook) {
                openBookHandler.run();
            } else {
                addBookHandler.run();
            }
        });

        /* Summary ------------------------------------------------------ */
        booksCountLabel = new Label("0");
        pagesCountLabel = new Label("0");
        streakValueLabel = new Label("0");
        streakLabel = new Label("Sin racha de lectura");

        HBox stats = new HBox(12,
                statCard(booksCountLabel, "textos en tu biblioteca"),
                statCard(pagesCountLabel, "páginas en total"),
                statCard(streakValueLabel, streakLabel));
        stats.getStyleClass().add("stats-row");
        for (var child : stats.getChildren()) {
            HBox.setHgrow(child, Priority.ALWAYS);
            if (child instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }

        /* Library ------------------------------------------------------ */
        Label libraryTitle = new Label("Biblioteca");
        libraryTitle.getStyleClass().add("section-title");

        libraryCountLabel = new Label("0 textos");
        libraryCountLabel.getStyleClass().add("text-xs");

        Region librarySpacer = new Region();
        HBox.setHgrow(librarySpacer, Priority.ALWAYS);

        HBox libraryHeader = new HBox(8, libraryTitle, librarySpacer, libraryCountLabel);
        libraryHeader.getStyleClass().add("library-header");
        libraryHeader.setAlignment(Pos.CENTER_LEFT);

        booksList = new ListView<>();
        booksList.getStyleClass().add("library-list");
        booksList.setCellFactory(list -> new BookCell());
        booksList.setPrefHeight(220);
        VBox.setVgrow(booksList, Priority.ALWAYS);

        Label emptyLibrary = new Label("Todavía no hay textos. Crea el primero con «Nuevo texto».");
        emptyLibrary.getStyleClass().add("empty-title");
        booksList.setPlaceholder(emptyLibrary);

        booksList.setOnMouseClicked(event -> {
            selectedBook = booksList.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                openBookHandler.run();
            }
        });

        VBox libraryCard = new VBox(0, libraryHeader, booksList);
        libraryCard.getStyleClass().addAll("card", "library-card");
        VBox.setVgrow(libraryCard, Priority.ALWAYS);

        content.getChildren().addAll(header, continueCard, stats, libraryCard);
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().add(content);
    }

    /** Builds one of the small summary cards. */
    private VBox statCard(Label value, String caption) {
        value.getStyleClass().add("stat-value");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-label");
        return statCard(value, captionLabel);
    }

    /** Builds one of the small summary cards. */
    private VBox statCard(Label value, Label caption) {
        if (!value.getStyleClass().contains("stat-value")) {
            value.getStyleClass().add("stat-value");
        }
        caption.getStyleClass().add("stat-label");
        VBox card = new VBox(2, value, caption);
        card.getStyleClass().add("stat-card");
        return card;
    }

    /** Sets the books shown in the library list. */
    public void setBooks(java.util.List<Book> books) {
        booksList.getItems().setAll(books);
        int pages = 0;
        for (Book book : books) {
            pages += book.getPageCount();
        }
        booksCountLabel.setText(String.valueOf(books.size()));
        pagesCountLabel.setText(String.valueOf(pages));
        libraryCountLabel.setText(books.size() == 1 ? "1 texto" : books.size() + " textos");
    }

    /** Returns the list of books of the library. */
    public ListView<Book> getBooksList() {
        return booksList;
    }

    /** Returns the label holding the title of the book being read. */
    public Label getBookTitleLabel() {
        return bookTitleLabel;
    }

    /** Returns the label holding the number of words of the book. */
    public Label getWordCountLabel() {
        return wordCountLabel;
    }

    /** Returns the label holding the reading progress. */
    public Label getProgressLabel() {
        return progressLabel;
    }

    /** Returns the label holding the reading streak. */
    public Label getStreakLabel() {
        return streakLabel;
    }

    /** Updates the card of the book being read. */
    public void updateBookInfo(String title, int progress, int wordCount) {
        bookTitleLabel.setText(title);
        progressBar.setProgress(progress / 100.0);
        progressLabel.setText(progress + "%");
        wordCountLabel.setText(formatWordCount(wordCount));
        coverLetterLabel.setText(initialOf(title));
        hasBook = title != null && !title.isBlank() && !"Sin libro".equals(title);
    }

    /** Shows the reading streak. */
    public void setStreak(int streak) {
        streakLabel.setText(streak > 0
                ? "Racha de lectura: " + streak + (streak == 1 ? " día" : " días")
                : "Sin racha de lectura");
        streakValueLabel.setText(String.valueOf(streak));
    }

    /** Sets the handler called when a book is opened. */
    public void setOpenBookHandler(Runnable handler) {
        this.openBookHandler = handler;
    }

    /** Sets the handler called when a book is added. */
    public void setAddBookHandler(Runnable handler) {
        this.addBookHandler = handler;
    }

    /** Returns the book selected in the library list. */
    public Book getSelectedBook() {
        return selectedBook != null ? selectedBook : null;
    }

    public Button getAddButton() {
        return addButton;
    }

    private String initialOf(String title) {
        if (title == null || title.isBlank()) {
            return "L";
        }
        return title.trim().substring(0, 1).toUpperCase();
    }

    private String formatWordCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fk palabras", count / 1000.0);
        }
        return count + " palabras";
    }

    /** A row of the library list. */
    private class BookCell extends ListCell<Book> {
        @Override
        protected void updateItem(Book book, boolean empty) {
            super.updateItem(book, empty);
            if (empty || book == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Label initial = new Label(initialOf(book.getTitle()));
            initial.getStyleClass().add("book-tile-letter");
            StackPane tile = new StackPane(initial);
            tile.getStyleClass().add("book-tile");

            Label title = new Label(book.getTitle().isBlank() ? "Libro sin título" : book.getTitle());
            title.getStyleClass().add("library-title");
            title.setMaxWidth(Double.MAX_VALUE);

            String meta = (book.getLanguageName() == null || book.getLanguageName().isBlank()
                    ? "" : book.getLanguageName() + "  ·  ")
                    + book.getPageCount() + (book.getPageCount() == 1 ? " página" : " páginas");
            Label details = new Label(meta);
            details.getStyleClass().add("library-meta");

            VBox text = new VBox(2, title, details);
            HBox.setHgrow(text, Priority.ALWAYS);

            Label progress = new Label(book.getProgressPercent() + "%");
            progress.getStyleClass().add("library-progress");

            Label chevron = new Label("›");
            chevron.getStyleClass().add("library-meta");

            HBox row = new HBox(13, tile, text, progress, chevron);
            row.getStyleClass().add("library-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);

            setText(null);
            setGraphic(row);
        }
    }
}
