package com.leelo.view;

import com.leelo.model.Book;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests of the home screen.
 */
class HomeViewTest {

    @Test
    void buildsTheLibraryList() {
        HomeView view = new HomeView();
        Book book = new Book();
        book.setTitle("El principito");
        book.setLanguageName("French");
        book.setPageCount(12);

        view.setBooks(List.of(book));

        assertNotNull(view.getAddButton());
        assertEquals(1, view.getBooksList().getItems().size());
        assertEquals("El principito", view.getBooksList().getItems().get(0).getTitle());
    }

    @Test
    void showsTheProgressOfTheBook() {
        HomeView view = new HomeView();

        view.updateBookInfo("El principito", 40, 1200);

        assertEquals("El principito", view.getBookTitleLabel().getText());
        assertEquals("40%", view.getProgressLabel().getText());
        assertEquals(String.format(java.util.Locale.getDefault(), "%.1fk palabras", 1200 / 1000.0),
                view.getWordCountLabel().getText());
    }

    @Test
    void showsTheReadingStreak() {
        HomeView view = new HomeView();

        view.setStreak(7);

        assertEquals("Racha de lectura: 7 días", view.getStreakLabel().getText());
    }
}
