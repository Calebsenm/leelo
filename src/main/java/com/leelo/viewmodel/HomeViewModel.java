package com.leelo.viewmodel;

import com.leelo.model.Book;
import com.leelo.service.BookService;
import com.leelo.service.SettingService;
import com.leelo.service.StatisticsService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 * The state of the home screen: the books of the library and the progress of
 * the last one that was read.
 */
public class HomeViewModel {

    private final BookService bookService = new BookService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final SettingService settingService = new SettingService();

    private final ObservableList<Book> books = FXCollections.observableArrayList();
    private final ObjectProperty<Book> lastOpenedBook = new SimpleObjectProperty<>();
    private final StringProperty bookTitle = new SimpleStringProperty("Sin libro");
    private final IntegerProperty readingProgress = new SimpleIntegerProperty(0);
    private final IntegerProperty wordCount = new SimpleIntegerProperty(0);
    private final IntegerProperty streak = new SimpleIntegerProperty(0);
    private final BooleanProperty hasBook = new SimpleBooleanProperty(false);

    public HomeViewModel() {
        load();
    }

    /** Loads the library and the progress of the last book that was read. */
    public void load() {
        books.setAll(bookService.getAll(settingService.getCurrentLanguageId(), false));
        streak.set(statisticsService.getReadingStreak());

        Book last = null;
        for (Book book : books) {
            if (book.getCurrentPageId() > 0) {
                last = book;
                break;
            }
        }
        if (last == null && !books.isEmpty()) {
            last = books.get(0);
        }

        lastOpenedBook.set(last);
        hasBook.set(last != null);
        if (last == null) {
            bookTitle.set("Sin libro");
            readingProgress.set(0);
            wordCount.set(0);
            return;
        }

        bookTitle.set(last.getTitle().isBlank() ? "Libro sin título" : last.getTitle());
        int pageCount = Math.max(1, last.getPageCount());
        int currentPage = 0;
        for (var page : bookService.getPages(last.getId())) {
            if (page.getId() == last.getCurrentPageId()) {
                currentPage = page.getOrder() - 1;
                break;
            }
        }
        readingProgress.set(Math.min(100, (currentPage * 100) / pageCount));
        wordCount.set(last.getPageCount() * 250);
    }

    public ObservableList<Book> getBooks() {
        return books;
    }

    public ObjectProperty<Book> lastOpenedBookProperty() {
        return lastOpenedBook;
    }

    public StringProperty bookTitleProperty() {
        return bookTitle;
    }

    public IntegerProperty readingProgressProperty() {
        return readingProgress;
    }

    public IntegerProperty wordCountProperty() {
        return wordCount;
    }

    public IntegerProperty streakProperty() {
        return streak;
    }

    public BooleanProperty hasBookProperty() {
        return hasBook;
    }

    public Book getLastOpenedBook() {
        return lastOpenedBook.get();
    }

    public int getReadingProgress() {
        return readingProgress.get();
    }

    public String getBookTitle() {
        return bookTitle.get();
    }

    public boolean hasBook() {
        return hasBook.get();
    }

    public int getWordCount() {
        return wordCount.get();
    }

    public int getStreak() {
        return streak.get();
    }
}
