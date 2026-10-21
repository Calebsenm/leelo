package com.leelo.service;

import com.leelo.dao.TermTagDAO;
import com.leelo.model.TermTag;

import java.util.List;

/** Manages the tags that can be attached to a term. */
public class TermTagService {

    private final TermTagDAO termTagDAO = new TermTagDAO();

    /** Returns every term tag, with the number of terms carrying it. */
    public List<TermTag> getAll() {
        return termTagDAO.findAll();
    }

    /** Returns the tag with the given id, or {@code null}. */
    public TermTag getById(int id) {
        return termTagDAO.findById(id);
    }

    /** Returns the tag with the given text, creating it when it does not exist. */
    public TermTag findOrCreateByText(String text) {
        return termTagDAO.findOrCreateByText(text);
    }

    /** Creates or updates a tag. */
    public boolean save(TermTag tag) {
        if (tag.getText() == null || tag.getText().isBlank()) {
            return false;
        }
        if (tag.getId() > 0) {
            return termTagDAO.update(tag);
        }
        TermTag created = termTagDAO.findOrCreateByText(tag.getText().trim());
        if (created == null) {
            return false;
        }
        tag.setId(created.getId());
        return termTagDAO.update(tag);
    }

    /** Deletes a tag. */
    public boolean delete(int id) {
        return termTagDAO.delete(id);
    }
}
