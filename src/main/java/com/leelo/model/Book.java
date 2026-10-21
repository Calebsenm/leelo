package com.leelo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A book: an ordered collection of pages that are read in a single language.
 *
 * <p>Pages are the unit of reading; the book itself only holds metadata such
 * as the title, the language, the source it was imported from and the tags
 * used to group it with other books.
 */
public class Book {

    private int id;
    private int languageId;
    private String languageName = "";
    private String title = "";
    private String sourceUri = "";
    private boolean archived;
    private int currentPageId;
    private String creationDate = "";
    private List<BookTag> tags = new ArrayList<>();
    private int pageCount;
    private int currentPageNumber;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri == null ? "" : sourceUri;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    /** Id of the page the reader was last on. */
    public int getCurrentPageId() {
        return currentPageId;
    }

    public void setCurrentPageId(int currentPageId) {
        this.currentPageId = currentPageId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate == null ? "" : creationDate;
    }

    public List<BookTag> getTags() {
        return tags;
    }

    public void setTags(List<BookTag> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    /** Total number of pages; populated by listing queries. */
    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    /** Number of the page the reader was last on; populated by listing queries. */
    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public void setCurrentPageNumber(int currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    /** Percentage of the book that was read. */
    public int getProgressPercent() {
        if (pageCount <= 0) {
            return 0;
        }
        return Math.min(100, (currentPageNumber * 100) / pageCount);
    }

    /** Returns the tags as a single comma separated string. */
    public String getTagList() {
        StringBuilder builder = new StringBuilder();
        for (BookTag tag : tags) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(tag.getText());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return title;
    }
}
