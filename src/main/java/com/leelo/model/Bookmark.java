package com.leelo.model;

/** A bookmark pointing at a page of a book. */
public class Bookmark {

    private int id;
    private int pageId;
    private int bookId;
    private int pageNumber;
    private String title = "";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    /** One-based page number the bookmark points at. */
    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    @Override
    public String toString() {
        return title;
    }
}
