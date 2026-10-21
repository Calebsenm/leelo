package com.leelo.service;

import com.leelo.dao.BookTagDAO;
import com.leelo.model.BookTag;

import java.util.List;

/** Manages the tags that can be attached to a book. */
public class BookTagService {

    private final BookTagDAO bookTagDAO = new BookTagDAO();

    /** Returns every book tag. */
    public List<BookTag> getAll() {
        return bookTagDAO.findAll();
    }

    /** Returns the tag with the given text, creating it when it does not exist. */
    public BookTag findOrCreateByText(String text) {
        return bookTagDAO.findOrCreateByText(text);
    }

    /** Creates or updates a tag. */
    public boolean save(BookTag tag) {
        if (tag.getText() == null || tag.getText().isBlank()) {
            return false;
        }
        if (tag.getId() > 0) {
            return bookTagDAO.update(tag);
        }
        BookTag created = bookTagDAO.findOrCreateByText(tag.getText().trim());
        if (created == null) {
            return false;
        }
        tag.setId(created.getId());
        return bookTagDAO.update(tag);
    }

    /** Deletes a tag. */
    public boolean delete(int id) {
        return bookTagDAO.delete(id);
    }
}
