package com.leelo.controller;

import com.leelo.App;
import com.leelo.model.Book;
import com.leelo.view.HomeView;
import com.leelo.viewmodel.HomeViewModel;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * The home screen: the book being read and the library.
 */
public class HomeController {

    private HomeViewModel viewModel;
    private HomeView view;

    @FXML
    public void initialize() {
        if (view != null) {
            return;
        }
        this.viewModel = new HomeViewModel();
        this.view = new HomeView();
        bindViewModel();
    }

    /** Connects the view with the view model. */
    private void bindViewModel() {
        viewModel.bookTitleProperty().addListener((observable, oldValue, newValue) ->
                view.updateBookInfo(newValue, viewModel.getReadingProgress(), viewModel.getWordCount()));
        viewModel.readingProgressProperty().addListener((observable, oldValue, newValue) ->
                view.updateBookInfo(viewModel.getBookTitle(), newValue.intValue(), viewModel.getWordCount()));
        viewModel.wordCountProperty().addListener((observable, oldValue, newValue) ->
                view.updateBookInfo(viewModel.getBookTitle(), viewModel.getReadingProgress(), newValue.intValue()));
        viewModel.streakProperty().addListener((observable, oldValue, newValue) ->
                view.setStreak(newValue.intValue()));

        view.setOpenBookHandler(this::openSelectedBook);
        view.setAddBookHandler(this::addBook);

        view.updateBookInfo(viewModel.getBookTitle(), viewModel.getReadingProgress(), viewModel.getWordCount());
        view.setStreak(viewModel.getStreak());
        view.setBooks(viewModel.getBooks());
    }

    /** Opens the book selected in the library. */
    private void openSelectedBook() {
        Book book = view.getSelectedBook();
        if (book == null) {
            book = viewModel.getLastOpenedBook();
        }
        if (book == null) {
            return;
        }
        try {
            App.openReading(book);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Opens the screen used to add a book. */
    private void addBook() {
        try {
            App.setRoot("add_text");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Returns the view of the screen. */
    public StackPane getView() {
        initialize();
        StackPane root = new StackPane(view);
        root.getStyleClass().add("app-shell");
        return root;
    }
}
