package com.leelo.service;

import com.leelo.dao.BookmarkDAO;
import com.leelo.model.Bookmark;

import java.util.List;

/** Manages the bookmarks pointing at the pages of a book. */
public class BookmarkService {

    private final BookmarkDAO bookmarkDAO = new BookmarkDAO();

    /** Returns the bookmarks of a book, ordered by the page they point at. */
    public List<Bookmark> getByBook(int bookId) {
        return bookmarkDAO.findByBook(bookId);
    }

    /** Adds a bookmark to a page. */
    public int add(int pageId, String title) {
        Bookmark bookmark = new Bookmark();
        bookmark.setPageId(pageId);
        bookmark.setTitle(title == null ? "" : title);
        return bookmarkDAO.insert(bookmark);
    }

    /** Renames a bookmark. */
    public boolean rename(int bookmarkId, String title) {
        Bookmark bookmark = new Bookmark();
        bookmark.setId(bookmarkId);
        bookmark.setTitle(title == null ? "" : title);
        return bookmarkDAO.update(bookmark);
    }

    /** Deletes a bookmark. */
    public boolean delete(int id) {
        return bookmarkDAO.delete(id);
    }
}
