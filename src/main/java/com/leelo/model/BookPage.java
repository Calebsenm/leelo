package com.leelo.model;

import java.time.Instant;

/** A single page of a {@link Book}. */
public class BookPage {

    private int id;
    private int bookId;
    private int order;
    private String content = "";
    private Instant readDate;
    private Instant startDate;
    private int wordCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    /** One-based position of the page inside its book. */
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    /** When the page was marked as read, or {@code null} if it never was. */
    public Instant getReadDate() {
        return readDate;
    }

    public void setReadDate(Instant readDate) {
        this.readDate = readDate;
    }

    /** When the page was last opened. */
    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    /** Number of word tokens contained in the page. */
    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public boolean isRead() {
        return readDate != null;
    }

    @Override
    public String toString() {
        return "Page " + order;
    }
}
